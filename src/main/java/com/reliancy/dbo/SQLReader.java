/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import com.reliancy.dbo.Action.Load;


/** SQLIterator will delay closing a connection and will iterate over result set.
 * TODO: no support for orderby yet
 */
public class SQLReader implements SiphonIterator<DBO>{
    protected final Entity entity;
    protected final SQLTerminal terminal;
    protected final SQL sql;
    protected FieldSlice slice;
    protected ResultSet result;
    protected Exception error;

    public SQLReader(Entity ent,SQLTerminal t) {
        this.entity=ent;
        terminal=t;
        // slice controls sql fields but also lets us correctly import values later
        slice=new FieldSlice(entity).including(Field.FLAG_STORABLE);
        sql=new SQL(terminal);
    }
    public SQLReader open() throws SQLException{
        return open(null);
    }
    public SQLReader open(Action action) throws SQLException{
        error=null;
        if(action==null){
            sql.select(entity,slice); // simple case
        }else{
            compileRecipe(action);  // complete case
        }
        //System.out.println("SQL:"+sql);
        Connection link=terminal.getConnection();
        PreparedStatement prep=link.prepareStatement(sql.toString());
        if(action!=null){
            Load tr=(Load) action.getTrait();
            if(tr.filter!=null){
                ArrayList<Object> params=new ArrayList<>();
                sql.check_export(tr.filter, params);
                for(int pindex=0;pindex<params.size();pindex++){
                    Object val=params.get(pindex);
                    prep.setObject(pindex+1,val);
                }
            }
        }

        result=prep.executeQuery();
        if(link.getAutoCommit()==false) link.commit();
        //action.setItems(this); -- maybe we want multiple readers on same actions - leave this to terminal
        return this;
    }
    public SQL compileRecipe(Action action){
        sql.select(action.getEntity(),slice);
        Load tr=(Load) action.getTrait();
        if(tr.filter!=null){
            sql.where(tr.filter);
        }
        return sql;
    }
    @Override
    public boolean hasNext() {
        try {
            return error==null?result.next():false;
        } catch (SQLException e) {
            error=e;
            return false;
        }
    }

    @Override
    public DBO next() {
        try {
            DBO ret=(DBO) slice.makeRecord();
            FieldSlice fit=slice.rewind();
            while(fit.hasNext()){
                int findex=fit.nextIndex();
                //Field field=fit.next();
                Object val=result.getObject(findex+1);
                fit.writeRecord(ret, val);
            }
            ret.setStatus(DBO.Status.USED);
            return ret;
        } catch (Exception e) {
            error=e;
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        if(result!=null){
            Statement stmt=null;
            Connection link=null;
            try{
                stmt=result.getStatement();
                link=stmt!=null?stmt.getConnection():null;
                if(!result.isClosed()) result.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
            try{
                if(link!=null) link.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
        }
        if(error!=null){
            if(error instanceof IOException) throw (IOException)error;
            else throw new IOException(error);
        }
    }
    
}
