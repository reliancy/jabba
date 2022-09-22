/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.IOException;

/** Endpoint for dbo objects.
 *  this interface will implemet CRUD plus control over a databse or folder.
 *  control will be implemented via meta terminal which will return specialized terminals for each entity and running actions on it
 *  will modify the entity structure.
 * 
 *  the core of the temrminal will be the Action object. The others will just be wrappers for since item actions.
 *  the action will be a read or write query with session management. 
 */
public interface Terminal {
    public Action execute(Action q) throws IOException;
    
    public default Action begin(){
        return begin(null);
    }
    public default Action begin(String sig){
        return new Action(this);
    }
    public default void end(Action act){
        act.clear();
    }
    public default Terminal meta(Entity ent){
        return null;
    }
    public default <T extends DBO> T load(Class<T> cls,Object...id) throws IOException {
        Entity ent=Entity.recall(cls);
        String sig="/"+ent.getName()+"/load";
        try(Action act=begin(sig).load(ent).limit(1).if_pk(id).execute()){
            return cls.cast(act.first());
        }
    }
    public default DBO load(Entity ent,Object...id) throws IOException {
        String sig="/"+ent.getName()+"/load";
        try(Action act=begin(sig).load(ent).limit(1).if_pk(id).execute()){
            return act.first();
        }
    }
    public default boolean save(DBO rec) throws IOException{
        Entity ent=rec.getType();
        String sig="/"+ent.getName()+"/save";
        try(Action act=begin(sig).save(ent).setItems(rec).execute()){
            return act.isDone();
        }
    }
    public default boolean delete(DBO rec) throws IOException {
        if(rec==null) return false;
        Entity ent=rec.getType();
        String sig="/"+ent.getName()+"/delete";
        try(Action act=begin(sig).delete(ent).setItems(rec).execute()){
            return act.isDone();
        }
    }
}
