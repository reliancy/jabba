package com.reliancy.dbo;

import java.util.HashMap;

import com.reliancy.rec.Hdr;

/** Describes an object structure, usually a table.
 * 
 */
public class Entity extends Hdr{
    static final HashMap<String,Entity> registry=new HashMap<>();
    public static final void publish(Entity ent){
        registry.put(ent.getName(),ent);
    }
    public static final void retract(Entity ent){
        registry.values().remove(ent);
    }
    public static final Entity recall(String name){
        return registry.get(name);
    }
    public static final Entity recall(Class<?> cls){
        return recall(cls.getSimpleName());
    }
    /**
     * this method will analyze a DBO class and forumate an Entity object out of it.
     * @param cls
     * @return
     */
    public static final Entity publish(Class<? extends DBO> cls){
        return null;
    }
    Entity base;
    String dbName;
    Field pk;
    public Entity(String name) {
        super(name);
    }
    public Field getPk(){
        return pk;
    } 
}
