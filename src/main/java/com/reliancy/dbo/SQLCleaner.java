/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

/** Helper object which impleents DBO deleting. 
 * It manages the recipe and the prepared statmenet.
 * The cleaner works in two ways.
 * If you supply items iterator it will delete by pk id those items.
 * If you supply a Check filter and no items then it will delete based on a where statement.
 */
public class SQLCleaner implements Closeable{
    protected final Entity entity;
    protected final SQLTerminal terminal;
    protected final SQLCleaner base;   /// used for nesting
    protected final SQL sql;
    protected final ArrayList<Object> params;
    protected Check filter;
    protected Connection external;
    protected PreparedStatement deleteStmt;
    protected int itemsDeleted;
    protected Exception error;

    public SQLCleaner(Entity ent,SQLTerminal t) {
        entity=ent;
        terminal=t;
        base=(entity.getBase()!=null)?new SQLCleaner(entity.getBase(),t):null;
        sql=new SQL(terminal);
        params=new ArrayList<>();
    }
    public SQL compileRecipe(){
        if(filter==null){
            // no filter we go with PK
            Field pk=entity.getPk();
            filter=pk.eq("?");
        }
        sql.delete(entity).where(filter);
        return sql;
    }
    public boolean isLinkExternal(){
        return external!=null;
    }
    public SQLCleaner setExternalLink(Connection link){
        external=link;
        return this;
    }
    protected Connection getExternalLink(){
        return external;
    }
    protected Connection getInternalLink(){
        try{
            if(deleteStmt!=null) return deleteStmt.getConnection();
        }catch(SQLException ex){
        }
        return null;
    }
    public SQLCleaner open() throws SQLException{
        return open(null);
    }
    public SQLCleaner open(Check where) throws SQLException{
        this.filter=where;
        Connection link=isLinkExternal()?getExternalLink():terminal.getConnection();
        if(base!=null) base.setExternalLink(link).open(filter); // definitely external link for base
        SQL delSQL=compileRecipe();
        //System.out.println("DEL:"+delSQL+"/"+filter);
        deleteStmt=link.prepareStatement(delSQL.toString());
        return this;
    }
    @Override
    public void close() throws IOException{
        if(base!=null) base.close(); // since link is external it will not close link just the rest
        Connection link=getInternalLink();
        if(deleteStmt!=null){
            try{
                deleteStmt.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
        }
        try{
            if(link!=null && !isLinkExternal()) link.close();
            external=null;
        }catch(SQLException ex){
            if(error==null) error=ex;
        }
        if(error!=null){
            if(error instanceof IOException) throw (IOException)error;
            else throw new IOException(error);
        }

    }
    public void flush(Iterator<DBO> items) throws SQLException {
        Connection link=getInternalLink();
        boolean autocommited=link.getAutoCommit();
        try{
            link.setAutoCommit(false);
            if(items==null){ // deleting by filter
                throw new SQLException("delete by filter not implemented yet");
                // we would need to leave the primary filter
                // we would use filter in an ID in (SUBQUERY)
                // this is because filter could reference all entities and we have inheritance so multiple
                // we would generate a select statement with filter and selecting only ID
            }else{ // deleting by incoming records
                while(items.hasNext()){
                    DBO rec=items.next();
                    deleteRecord(rec);
                }
            }
            if(!link.getAutoCommit()){
                link.commit();
            }
        }catch(SQLException ex){
            if(!link.getAutoCommit()){
                link.rollback();
            }
            throw ex;
        }finally{
            link.setAutoCommit(autocommited);
        }
    }
    /**
     * This calls one delete. It can and is called from outside in case of nesting when link is external.
     * @param rec
     * @throws SQLException
     */
    public boolean deleteRecord(DBO rec) throws SQLException{
        if(rec==null) return false;
        if(base!=null) base.deleteRecord(rec); // save the superclass first
        sql.check_import(filter,rec); // get values from dbo
        params.clear();
        sql.check_export(filter,params); // move them into params
        for(int pindex=0;pindex<params.size();pindex++){
            Object val=params.get(pindex);
            deleteStmt.setObject(pindex+1,val);
        }
        int dcode=deleteStmt.executeUpdate();
        itemsDeleted+=dcode;
        return dcode>0;
    }
}
