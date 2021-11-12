package com.reliancy.dbo;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import com.reliancy.util.Handy;

/** Helper object which impleents DBO saving. It manages the recipe and the prepared statmenet.
 * Also keeps track of which fields are sent down to DB.
 * The writer does the work in flush method during which it exhausts the items. While in flush it 
 * will disable autocommit and enable it at the end. We could call flush multiple times to send multiple
 * batches down to DB.
 * Initially we would ctor with Action object but actually we need to generate writes for different entities
 * especially in inheritance cases.
 */
public class SQLWriter implements Closeable{
    protected final Entity entity;
    protected final SQLTerminal terminal;
    protected final SQLWriter base;   /// used for nesting
    protected final ArrayList<Field> supplied=new ArrayList<Field>();
    protected final ArrayList<Field> generated=new ArrayList<Field>();
    protected String insertSQL;
    protected String updateSQL;
    protected Connection external;
    protected PreparedStatement insertStmt;
    protected PreparedStatement updateStmt;
    protected int itemsInserted;
    protected int itemsUpdated;
    protected Exception error;

    public SQLWriter(Entity ent,SQLTerminal t) {
        entity=ent;
        terminal=t;
        base=(entity.getBase()!=null)?new SQLWriter(entity.getBase(),t):null;
        // we select proper fields for this entity
        FieldSlice slice=new FieldSlice(entity).including(Field.FLAG_STORABLE); // includes all even autoincrement
        while(slice.hasNext()){
            Field f=slice.next();
            Entity e=slice.nextEntity();
            if(e!=entity) continue; // skip if not part of this entity
            if(f.isAutoIncrement()){
                generated.add(f);
            }else{
                supplied.add(f);
            }
        }
    }
    public String compileInsertRecipe(){
        if(insertSQL!=null) return insertSQL;
        SQL buf=new SQL(terminal);
        buf.insert(entity,supplied);
        insertSQL=buf.toString();
        return insertSQL;
    }
    public String compileUpdateRecipe(){
        if(updateSQL!=null) return updateSQL;
        SQL buf=new SQL(terminal);
        buf.update(entity,supplied);
        updateSQL=buf.toString();
        return updateSQL;
    }
    public boolean isLinkExternal(){
        return external!=null;
    }
    public SQLWriter setExternalLink(Connection link){
        external=link;
        return this;
    }
    protected Connection getExternalLink(){
        return external;
    }
    protected Connection getInternalLink(){
        try{
            if(insertStmt!=null) return insertStmt.getConnection();
            if(updateStmt!=null) return updateStmt.getConnection();
        }catch(SQLException ex){
        }
        return null;
    }
    public SQLWriter open() throws SQLException{
        Connection link=isLinkExternal()?getExternalLink():terminal.getConnection();
        if(base!=null) base.setExternalLink(link).open(); // definitely external link for base
        String inSql=compileInsertRecipe();
        String upSql=compileUpdateRecipe();
        //System.out.println("INS:"+inSql);
        //System.out.println("UPD:"+upSql);
        String[] genkeys=new String[generated.size()];
        for(int i=0;i<generated.size();i++){
            Field f=generated.get(i);
            genkeys[i]=f.getName();
        }
        insertStmt=link.prepareStatement(inSql,genkeys);
        updateStmt=link.prepareStatement(upSql);
        //result=prep.executeQuery();
        //if(link.getAutoCommit()==false) link.commit();
        return this;
    }
    @Override
    public void close() throws IOException{
        if(base!=null) base.close(); // since link is external it will not close link just the rest
        Connection link=getInternalLink();
        if(insertStmt!=null){
            try{
                insertStmt.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
        }
        if(updateStmt!=null){
            try{
                updateStmt.close();
            }catch(SQLException ex){
                if(error==null) error=ex;
            }
        }
        try{
            if(link!=null && external!=link) link.close();
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
        Connection link=isLinkExternal()?getExternalLink():getInternalLink();
        boolean autocommited=link.getAutoCommit();
        try{
            link.setAutoCommit(false);
            while(items.hasNext()){
                DBO rec=items.next();
                writeRecord(rec);
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
     * This calls one update/insert. It can and is called from outside in case of nesting when link is external.
     * @param rec
     * @throws SQLException
     */
    public boolean writeRecord(DBO rec) throws SQLException{
        if(base!=null) base.writeRecord(rec); // save the superclass first
        // select mode
        int pindex=0;
        Field pk=entity.getPk();
        boolean pk_owned=entity.isOwned(pk);
        PreparedStatement stmt=null;
        if(rec.getStatus()==DBO.Status.NEW){
            stmt=insertStmt;
            // need to inject pk here is not owned
            if(!pk_owned) stmt.setObject(++pindex,pk.get(rec,null),terminal.getTypeId(pk.getType(),pk.getTypeParams()));
        }
        if(rec.getStatus()==DBO.Status.USED){
            stmt=updateStmt;
            // update has a pk condition for sure
            stmt.setObject(supplied.size()+1,pk.get(rec,null),terminal.getTypeId(pk.getType(),pk.getTypeParams()));
        }
        if(stmt==null) return false;
        // copy values
        for(int index=0;index<supplied.size();index++){
            Field f=supplied.get(index);
            pindex+=1;
            int tid=terminal.getTypeId(f.getType(),f.getTypeParams());
            Object val=f.get(rec,null);
            //System.out.println("Param:"+pindex+":"+f.getName()+":"+val);
            stmt.setObject(pindex,val,tid);
        }
        int ucode=stmt.executeUpdate();
        //System.out.println("UCode:"+ucode);
        if(rec.getStatus()==DBO.Status.NEW){
            this.itemsInserted+=ucode;
            if(ucode>0 && !generated.isEmpty()){
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if(keys.next()){
                        for(int i=0;i<generated.size();i++){
                            Field f=generated.get(i);
                            Object autoval=keys.getObject(i+1);
                            f.set(rec,autoval);
                        }
                    }
                }                
            }
        }
        if(rec.getStatus()==DBO.Status.USED){
            this.itemsUpdated+=ucode;
        }
        return ucode>0;
    }
}
