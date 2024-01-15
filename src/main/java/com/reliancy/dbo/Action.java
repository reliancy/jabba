/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/** Description of a terminal operation with a slice of dbo objects as input or output.
 * This object is not just for reading but also bulk updating.
 * It will be used to describe a multi DBO read or write and to then also track results.
 * At its core are action traits which are classes that define either loading,saving or deleting.
 * The items field is a consumable object when consumed the action is done.
 * So for loading we iterate once done it cannot be done again. Also when items are provided for saving
 * once iterated over and saved we are done.
 */
public class Action implements Iterable<DBO>,SiphonIterator<DBO>{
    public static class Trait{
        public String toString(){return getClass().getSimpleName();}
    }
    public static class Load extends Trait{
        int limit,offset;
        Check filter;
    }
    public static class Save extends Trait{

    }
    public static class Delete extends Trait{
        Check filter;
    }

    Terminal terminal;
    Trait trait;
    Entity entity;
    Object[] params;
    SiphonIterator<DBO> items;

    public Action(){
        trait=null;
    }
    public Action(Trait t){
        trait=t;
    }
    public Action(Terminal t){
        terminal=t;
        trait=null;
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
    public Trait getTrait() {
        return trait;
    }
    public Action setTrait(Trait t) {
        this.trait = t;
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
        trait=null;
        entity=null;
        setItems((DBO)null);
    }
    public Action load(Entity ent){
        trait=new Load();
        entity=ent;
        return this;
    }
    public Action load(Class<? extends DBO> cls){
        trait=new Load();
        entity=Entity.recall(cls);
        return this;
    }
    public Action save(Entity ent){
        trait=new Save();
        entity=ent;
        return this;
    }
    public Action delete(Entity ent){
        trait=new Delete();
        entity=ent;
        return this;
    }
    public Action params(Object...p){
        params=p;
        return this;
    }
    public Action setItems(final DBO ...itms){
        SiphonIterator<DBO> it=null;
        if(itms!=null){
            it=new SiphonIterator<DBO>() {
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
    public Action setItems(final Collection<DBO> itms){
        SiphonIterator<DBO> it=null;
        if(itms!=null){
            it=new SiphonIterator<DBO>() {
                private final Iterator<DBO> str = itms.iterator();
                @Override
                public boolean hasNext() {
                    return str.hasNext();
                }
                @Override
                public DBO next() {
                    return str.next();
                }
                @Override
                public void close() throws IOException {
                }
            };
        }
        return setItems(it);
    }
    public Action setItems(SiphonIterator<DBO> itms){
        if(items==itms) return this;
        if(items!=null){
            try {
                items.close();
            } catch (Exception e) {
            }
        }
        items=itms;
        return this;
    }
    protected SiphonIterator<DBO> getItems(){
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
            items=null;
            if(terminal!=null) terminal.end(this);
        }
    }
    public Action limit(int max) {
        ((Load)trait).limit=max;
        return this;
    }
    public Action filterBy(Check... c){
        Check filter=null;
        if(c!=null){
            if(c.length>1) filter=Check.and(c);
            else filter=c[0];
        }
        if(trait instanceof Load){
            ((Load)trait).filter=filter;
        }else
        if(trait instanceof Delete){
            ((Delete)trait).filter=filter;
        }else{
            throw new IllegalStateException("filtering not supported by trait:"+trait);
        }
        return this;
    }
    public Check getFilter(){
        if(trait instanceof Load){
            return ((Load)trait).filter;
        }else
        if(trait instanceof Delete){
            return ((Delete)trait).filter;
        }else{
            throw new IllegalStateException("filtering not supported by trait:"+trait);
        }
    }
    public Action if_pk(Object... id) {
        Field pk=entity.getPk();
        return filterBy(pk.eq(id));
    }
    public DBO first() {
        try{
            if(this.hasNext()){
                return this.next();
            }else{
                return null;
            }
        }finally{
            clear();
        }
    }
    public boolean isDone(){
        return items==null || items.hasNext()==false;
    }
}
