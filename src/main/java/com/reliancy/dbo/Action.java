package com.reliancy.dbo;

import java.io.IOException;
import java.util.Iterator;

import com.reliancy.util.CloseableIterator;

/** Description of a terminal operation with a slice of dbo objects as input or output.
 * This object is not just for reading but also bulk updating.
 * It will be used to describe a multi DBO read or write and to then also track results.
 */
public class Action implements Iterable<DBO>,CloseableIterator<DBO>{
    public static enum Type{
        NONE,LOAD,SAVE,DELETE
    }
    Terminal terminal;
    Type type;
    Entity entity;
    Object[] params;
    CloseableIterator<DBO> items;
    int limit,offset;
    Condition filter;

    public Action(){
        type=Type.NONE;
    }
    public Action(Type t){
        type=t;
    }
    public Action(Terminal t){
        terminal=t;
        type=Type.NONE;
    }
    public Action execute() throws IOException{
        return terminal.execute(this);
    }

    public Terminal getTerminal() {
        return terminal;
    }
    public Action setTerminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }
    public Type getType() {
        return type;
    }
    public Action setType(Type type) {
        this.type = type;
        return this;
    }
    public Entity getEntity() {
        return entity;
    }
    public Action setEntity(Entity entity) {
        this.entity = entity;
        return this;
    }
    public void clear(){
        terminal=null;
        type=Type.NONE;
        entity=null;
        setItems((DBO)null);
    }
    public Action load(Entity ent){
        type=Type.LOAD;
        entity=ent;
        return this;
    }
    public Action save(Entity ent){
        type=Type.SAVE;
        entity=ent;
        return this;
    }
    public Action delete(Entity ent){
        type=Type.DELETE;
        entity=ent;
        return this;
    }
    public Action params(Object...p){
        params=p;
        return this;
    }
    public Action setItems(DBO ...itms){
        CloseableIterator<DBO> it=null;
        if(itms!=null){
            it=new CloseableIterator<DBO>() {
                private int index = 0;
                @Override
                public boolean hasNext() {
                    return itms.length > index;
                }
                @Override
                public DBO next() {
                    return itms[index++];
                }
                @Override
                public void close() throws IOException {
                }
            };
        }
        return setItems(it);
    }
    public Action setItems(CloseableIterator<DBO> itms){
        if(items!=null){
            try {
                items.close();
            } catch (Exception e) {
            }
        }
        items=itms;
        return this;
    }
    protected CloseableIterator<DBO> getItems(){
        return items;
    }
    @Override
    public Iterator<DBO> iterator() {
        return this;
    }
    @Override
    public boolean hasNext() {
        return items!=null?items.hasNext():false;
    }
    @Override
    public DBO next() {
        return items.next();
    }
    @Override
    public void close() throws IOException {
        if(items!=null){
            items.close();
            if(terminal!=null) terminal.end(this);
        }
        items=null;
    }
    public Action limit(int max) {
        limit=max;
        return this;
    }
    public Action if_filter(Condition... c){
        if(c!=null){
            if(c.length>1) filter=Condition.and(c);
            else filter=c[0];
        }else{
            filter=null;
        }
        return this;
    }
    public Action if_pk(Object[] id) {
        Field pk=entity.getPk();
        return if_filter(Condition.eq(pk,id));
    }
    public DBO first() {
        try{
            return items!=null?items.next():null;
        }finally{
            clear();
        }
    }
    public boolean isDone(){
        return items==null || items.hasNext()==false;
    }
}
