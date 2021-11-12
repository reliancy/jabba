package com.reliancy.dbo;

import java.io.IOException;

import com.reliancy.rec.Hdr;
import com.reliancy.rec.JSON;
import com.reliancy.rec.Rec;
import com.reliancy.rec.Slot;

/** Instance of an entity, usually a row in a table.
 * 
 */
public class DBO implements Rec{
    public static enum Status{
        NEW,USED,DELETED,COMPUTED
    }
    Terminal terminal;
    Entity type;
    Status status;
    Object[] values;

    public DBO() {
        Class<? extends DBO> cls=this.getClass();
        if(cls!=DBO.class){
            Entity ent=Entity.recall(cls);
            setType(ent);
        }
        status=Status.NEW;
    }
    @Override
    public String toString(){
        try {
            StringBuffer ret=new StringBuffer();
            JSON.writes(this,ret);
            return ret.toString();
        } catch (IOException e) {
            return e.toString();
        }
    }
    public Terminal getTerminal() {
        return terminal;
    }
    public DBO setTerminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }
    public Status getStatus(){
        return status;
    }
    public DBO setStatus(Status s) {
        this.status = s;
        return this;
    }
    public final Entity getType() {
        return type;
    }
    public final DBO setType(Entity type) {
        this.type = type;
        if(type==null){
            values=null;
        }else{
            values=new Object[type.count()];
        }
        return this;
    }
    @Override
    public Hdr meta() {
        return type;
    }
    @Override
    public int count() {
        return values!=null?values.length:0;
    }
    @Override
    public Rec set(int pos, Object val) {
        if(pos<0) pos=count()+pos;
        values[pos]=val;
        return this;
    }
    @Override
    public Object get(int pos) {
        if(pos<0) pos=count()+pos;
        return values[pos];
    }
    @Override
    public Rec add(Object val) {
        throw new UnsupportedOperationException("dbo is not array");
    }
    @Override
    public Rec remove(int s) {
        throw new UnsupportedOperationException("dbo is not array");
    }
    @Override
    public Rec set(Slot s, Object val) {
        if(s==null) throw new IllegalArgumentException("invalid key provided");
        int index=s.getPosition(); // try slot position
        //if(index<0) index=type.findSlot(s.getName());// fall back to search if slot not set
        if(index<0){
            throw new IllegalArgumentException("invalid key provided:"+s.getName());
        }else{
            values[index]=val;
        }
        return this;
    }
    @Override
    public Object get(Slot s, Object def) {
        if(s==null) throw new IllegalArgumentException("invalid key provided");
        int index=s.getPosition(); // try slot position
        //if(index<0) index=type.findSlot(s.getName());// fall back to search if slot not set
        if(index<0) throw new IllegalArgumentException("invalid key provided:"+s.getName());
        Object ret=values[index];
        return ret==null?def:ret;
    }
    @Override
    public Rec remove(Slot s) {
        throw new UnsupportedOperationException("dbo is not resizable");
    }

}
