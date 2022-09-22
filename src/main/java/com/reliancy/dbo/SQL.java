/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reliancy.util.Handy;

public final class SQL implements Appendable{
    public final static Object NULL=new Object();
    public final static String WS=" ";
    public final static String SELECT="SELECT";
    public final static String INSERT="INSERT INTO";
    public final static String UPDATE="UPDATE";
    public final static String DELETE="DELETE";
    public final static String FROM="FROM";
    public final static String INNER_JOIN="INNER JOIN";
    public final static String ON="ON";
    public final static String SET="SET";
    public final static String WHERE="WHERE";

    final StringBuffer buffer=new StringBuffer();
    final SQLTerminal terminal;
    final String ql,qr;
    final HashMap<Entity,String> entAlias=new HashMap<>();
    
    public SQL(SQLTerminal terminal){
        this.terminal=terminal;
        ql=terminal!=null?terminal.getQuoteLeft():"\"";
        qr=terminal!=null?terminal.getQuoteRight():"\"";
    }
    @Override
    public String toString(){
        return buffer.toString();
    }
    @Override
    public final SQL append(CharSequence csq){
        buffer.append(csq);
        return this;
    }
    @Override
    public final SQL append(CharSequence csq, int start, int end){
        buffer.append(csq,start,end);
        return this;
    }
    @Override
    public final SQL append(char c){
        buffer.append(c);
        return this;
    }
    public final SQL select(){
        append(SELECT);
        return this;
    }
    public final SQL insert(){
        append(INSERT);
        return this;
    }
    public final SQL update(){
        append(UPDATE);
        return this;
    }
    public final SQL delete(){
        append(DELETE);
        return this;
    }
    public final SQL from(){
        append(WS).append(FROM).append(WS);
        return this;
    }
    public final SQL inner_join(){
        append(WS).append(INNER_JOIN).append(WS);
        return this;
    }
    public final SQL on(){
        append(WS).append(ON).append(WS);
        return this;
    }
    public final String wrap(String id){
        if(id.startsWith(ql) && id.endsWith(qr)){
            return id;
        }else{
            return ql+id.replace(".",qr+"."+ql)+qr;
        }
    }
    public final SQL id(String id){
        if(id.startsWith(ql) && id.endsWith(qr)){
            append(id);
        }else{
            append(ql).append(id.replace(".",qr+"."+ql)).append(qr);
        }
        return this;
    }
    public final String getAlias(Entity e){
        String eAlias=entAlias.get(e);
        if(eAlias!=null) return eAlias;
        eAlias="e"+entAlias.size();
        entAlias.put(e,eAlias);
        return eAlias;
    }
    public final SQL select(Entity ent,FieldSlice fit){
        entAlias.clear();;
        select();
        while(fit.hasNext()){
            int index=fit.nextIndex();
            Field f=fit.next();
            Entity e=fit.nextEntity();
            String alias=getAlias(e);
            //System.out.println("It:"+index+":/"+f+"/"+e+"/"+alias);
            append(index==0?" ":",");
            append(alias).append(".").id(f.getName());
        }
        from();
        String eAlias=getAlias(ent);
        id(ent.getName()).append(" ").append(eAlias);
        for(Entity b=ent.getBase();b!=null;b=b.getBase()){
            String bAlias=getAlias(b);
            inner_join();
            id(b.getName()).append(" ").append(bAlias);
            on();
            Field bPk=b.getPk();
            Field ePk=ent.getPk();
            append(eAlias).append(".").id(ePk.getName());
            append("=");
            append(bAlias).append(".").id(bPk.getName());
        }
        return this;
    }
    public final SQL where(){
        append(WS).append(WHERE).append(WS);
        return this;
    }
    public final SQL where(Check filter) {
        append(WS).append(WHERE).append(WS).check(filter);
        return this;
    }
    /// using entalias locate field entity and its prefix
    public final String getFieldPrefix(Field f){
        for(Map.Entry<Entity,String> e:entAlias.entrySet()){
            Entity ent=e.getKey();
            String prefix=e.getValue();
            if(ent.isOwned(f)) return prefix+".";
        }
        return "";
    }
    public final SQL check(Check filter) {
        if(filter.isLeaf()){
            Check.Op op=filter.getCode();
            Field field=filter.getField();
            String fname=wrap(filter.getField().getName());
            String opname=op.toString();
            String arg="?";
            Object val=filter.getValue();
            if(op==Check.LIKE && terminal!=null && terminal.getProtocol().contains(":postgre")){
                opname="ILIKE";
            }
            if(op==Check.IN){
                arg="("+Handy.toString(val)+")";
            }
            if(Handy.isEmpty(val)){
                // if not value then shortcuircuid condition
                fname="1";
                opname="=";
                arg="1";
            }
            if(val==NULL){
                arg="NULL";
                if(op==Check.EQ) opname="IS";
                if(op==Check.NEQ) opname="IS NOT";
            }
            append("(");
            String fprefix=getFieldPrefix(field);
            append(fprefix).append(fname).append(WS).append(opname).append(WS).append(arg);
            append(")");
        }else{
            Check.Op op=filter.getCode();
            String delim=op.toString();
            if(op==Check.NOT){
                append(delim).append("(").check(filter.getChild(0)).append(")");
            }else{
                append("(");
                for(int i=0;i<filter.getChildCount();i++){
                    if(i>0) append(WS).append(delim).append(WS);
                    check(filter.getChild(i));
                }
                append(")");
            }
        }
        return this;
    }
    /** fills params list with non-trivial parameters.
     * we place this method here to be as close as possible to the one which generates the sql code.
     * check and check_export must be in synch.
     * @param filter
     * @param params
     */
    public final void check_export(Check filter,List<Object> params) {
        if(filter.isLeaf()){
            Check.Op op=filter.getCode();
            Object val=filter.getValue();
            if(Handy.isEmpty(val) || val==NULL || op==Check.IN) return; // skip over empty or NULL values
            params.add(val);
        }else{
            for(Check ch:filter) check_export(ch,params);
        }
    }
    /** fills check values from dbo record where equal and not-equal are used.
     * we place this method here to be as close as possible to the one which generates the sql code.
     * check and check_import must be in synch.
     * @param filter set of checks
     * @param rec record to check
     */
    public final void check_import(Check filter,DBO rec) {
        if(filter.isLeaf()){
            if(filter.isLocked()) return; // no import on locked condition
            Check.Op op=filter.getCode();
            if(op!=Check.EQ && op!=Check.NEQ) return; // skip over all conditions except = and <>
            Field f=filter.getField();
            Object val=f.get(rec,null);
            filter.setValue(val);
        }else{
            for(Check ch:filter) check_import(ch,rec);
        }
    }
    public final SQL insert(Entity entity,List<Field> supplied){
        insert();
        append(SQL.WS).id(entity.getName()).append(" (");
        StringBuffer ext=new StringBuffer();
        String delim="";
        Field pk=entity.getPk();
        if(!entity.isOwned(pk)){
            append(delim).id(pk.getName());
            ext.append(delim).append("?");
            delim=",";
        }
        for(int index=0;index<supplied.size();index++){
            Field f=supplied.get(index);
            if(index>0) delim=",";
            append(delim).id(f.getName());
            ext.append(delim).append("?");
        }
        append(") VALUES (").append(ext).append(")");
        return this;
    }
    public final SQL update(Entity entity,List<Field> supplied){
        update();
        append(SQL.WS).id(entity.getName()).append(" SET ");
        for(int index=0;index<supplied.size();index++){
            Field f=supplied.get(index);
            String delim=index==0?"":",";
            append(delim);
            id(f.getName()).append("=?");
        }
        where();
        Field pk=entity.getPk();
        id(pk.getName()).append("=?");
        return this;
    }
    public final SQL delete(Entity entity){
        delete().from().id(entity.getName());
        return this;
    }
}
