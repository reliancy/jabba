/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;
import java.util.Iterator;
import java.util.List;

/** Field iterator matching flags over entity hierarchy.
 * 
 */public class FieldSlice implements Iterator<Field>,Iterable<Field>{
    protected final Entity entity;
    protected FieldSlice sup;
    protected int includeMask;
    protected int excludeMask;
    protected int raw_index;
    protected Field next_field;
    protected int next_index;
    protected Entity next_entity;
    public FieldSlice(Entity ent){
        entity=ent;
        if(entity.getBase()!=null){
            sup=new FieldSlice(ent.getBase());
        }else{
            sup=null;
        }
        raw_index=-1;
        next_index=-1;
    }
    public FieldSlice including(int mask){
        includeMask=mask;
        if(sup!=null) sup.including(mask);
        return this;
    }
    public FieldSlice excluding(int mask){
        excludeMask=mask;
        if(sup!=null) sup.excluding(mask);
        return this;
    }
    /** we add rewind capability to allow reuse of same fieldslice.
     * i.e we use it to generate recipe, then later to properly enumerate values.
     */
    public FieldSlice rewind(){
        raw_index=-1;
        next_index=-1;
        next_field=null;
        next_entity=null;
        if(sup!=null) sup.rewind();
        return this;
    }
    // search for next valid field
    public final Field findNext(){
        List<?> local=entity.getOwnSlots();
        if(raw_index>=local.size()) return null; // we have exhausted local supply
        next_field=null; // clear prev result
        // search at base
        if(sup!=null && sup.hasNext()){
            next_field=sup.next();
            next_index++;
            next_entity=sup.nextEntity();
            return next_field;
        }
        next_entity=entity;
        // now search locally
        for(raw_index=raw_index+1;raw_index<local.size();raw_index++){
            Field f=(Field) local.get(raw_index);
            int attr=f.getFlags();
            if((attr & excludeMask)!=0) continue; // skip if in exluding set
            if((attr & includeMask)==0) continue; // skip if not in including set
            next_field=f;
            next_index+=1;
            break;
        }
        return next_field;
    }
    @Override
    public Iterator<Field> iterator() {
        return this;
    }
    @Override
    public boolean hasNext() {
        Field next=findNext();
        return next!=null;
    }

    @Override
    public Field next() {
        return next_field;
    }

    public int nextIndex(){
        return next_field!=null?next_index:-1;
    }
    public Entity nextEntity(){
        return next_entity;
    }
    public DBO makeRecord() throws InstantiationException, IllegalAccessException{
        return entity.newInstance();
    }
    public void writeRecord(DBO rec,Object val){
        rec.set(next_field, val);
    }
    public Object readRecord(DBO rec,Object def){
        return rec.get(next_field, def);
    }
}
