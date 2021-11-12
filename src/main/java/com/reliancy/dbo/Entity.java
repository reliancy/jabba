package com.reliancy.dbo;

import java.util.HashMap;
import java.util.Iterator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import com.reliancy.rec.Hdr;
import com.reliancy.rec.Slot;

/** Describes an object structure, usually a table.
 * 
 */
public class Entity extends Hdr{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Info {
        String name();
     }
    static final HashMap<String,Entity> registry=new HashMap<>();
    public static final void publish(Entity ent){
        registry.put(ent.getName(),ent);
        registry.put(ent.getId(),ent);
    }
    public static final void retract(Entity ent){
        if(ent==null) return;
        Collection<Entity> vals=registry.values();
        while(vals.remove(ent)){}
    }
    public static final void retract(Class<? extends DBO> cls){
        Entity ent=recall(cls.getSimpleName());
        if(ent!=null){
            retract(ent);
        }
    }
    public static final Entity recall(String name){
        return registry.get(name);
    }
    
    public static final Entity recall(Class<? extends DBO> cls){
        Entity ent=recall(cls.getSimpleName());
        if(ent==null){
            ent=publish(cls);
        }
        return ent;
    }
    /**
     * this method will analyze a DBO class and forumate an Entity object out of it.
     * @param cls
     * @return
     */
    public static final Entity publish(Class<? extends DBO> cls){
        Entity ret=registry.get(cls.getSimpleName());
        if(ret!=null) return ret;
        //System.out.println("Analyzing:"+cls);
        Class base=cls.getSuperclass();
        Entity base_ent=null;
        int position0=0;
        if(base!=null && base!=DBO.class){
            base_ent=publish(base);
            position0=base_ent.count();
        }
        java.lang.reflect.Field[] declaredFields = cls.getDeclaredFields();
        ArrayList<Field> slots=new ArrayList<>();
        for (java.lang.reflect.Field field : declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                String sf_name=field.getName();
                Field slot=(Field) field.get(cls);
                slot.setId(sf_name);
                slot.setPosition(position0+slots.size());
                slots.add(slot);
                //System.out.println(sf_name+":"+slot+" atpos:"+slot.getPosition());
            } catch (Exception e) {
            }
        }
        Info info=cls.getAnnotation(Info.class);
        ret=new Entity(info!=null?info.name():cls.getSimpleName()).setId(cls.getSimpleName());
        ret.setBase(base_ent);
        ret.setType(cls);
        ret.getOwnSlots().addAll(slots);
        publish(ret);
        return ret; 
    }
    Entity base;
    String id;
    Field pk;
    public Entity(String name) {
        super(name);
    }
    @Override
    public Slot makeSlot(String name){
        return new Field(name);
    }
    @Override
    public Iterator<Slot> iterator(int offset){
        if(offset>0) throw new IllegalArgumentException("Offset not supported");
        final Entity ent=this;
        return new Iterator<Slot>(){
            final FieldSlice slice=new FieldSlice(ent).including(Field.FLAG_STORABLE);
            @Override
            public boolean hasNext() {
                return slice.hasNext();
            }
            @Override
            public Slot next() {
                return slice.next();
            }

        };
    }
    @Override
    public int count(){
        return super.count()+(base!=null?base.count():0);
    }
    /**
     * gets a slot which could be here or in base.
     */
    @Override
    public Slot getSlot(int pos){
        if(base!=null){ // we got base
            int ofs=base.count();
            if(pos<ofs) return base.getSlot(pos);
            else return super.getSlot(pos-ofs);
        }else{ // regular no base
            return super.getSlot(pos);
        }
    }
    public Field getField(int index){
        return (Field)getSlot(index);
    }
    public int getDepth(){
        return base!=null?1+base.getDepth():0;
    }
    public Entity getBase() {
        return base;
    }
    public Entity setBase(Entity base) {
        this.base = base;
        return this;
    }
    public String getId() {
        return id;
    }
    public Entity setId(String id) {
        this.id = id;
        return this;
    }
    public Entity setPk(Field pk) {
        this.pk = pk;
        return this;
    }
    public Field getPk(){
        if(pk!=null) return pk;
        // try to locate the pk - this now gos over base as well
        for(int i=0;i<count() && pk==null;i++){
            Field pp=(Field) getSlot(i);
            if(pp.isPk()) pk=pp;
        }
        return pk;
    }
    public DBO newInstance() throws InstantiationException, IllegalAccessException{
        return newInstance(null).setStatus(DBO.Status.NEW);
    }
    public DBO newInstance(Terminal t) throws InstantiationException, IllegalAccessException{
        Class<?> cls=getType();
        DBO ret=(DBO) cls.newInstance();
        ret.setType(this).setTerminal(t).setStatus(DBO.Status.NEW);
        return ret;
    }    
}
