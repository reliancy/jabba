/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;

import com.reliancy.util.Handy;

public interface Config extends Iterable<Config.Property<?>>{
    public static class Property<V> {

        final String name;
        final Class<V> typ;
        V initial;
        boolean required;
        boolean writable;
        boolean persistent;
        public Property(String name,Class<V> typ){
            this.name=name;
            this.typ=typ;
            required=false;
            writable=true;
            persistent=false;
        }
        @Override
        public String toString(){
            return String.format("%s:%s",name,typ.getSimpleName());
        }
        public String getName(){return name;}
        public Class<V> getTyp(){return typ;}
        public Property<V> setInitial(V val){initial=val;return this;}
        public V getInitial(){return initial;}
        public Property<V> setRequired(boolean f){required=f;return this;}
        public boolean isRequired(){return required;}
        public Property<V> setWritable(boolean f){writable=f;return this;}
        public boolean isWritable(){return writable;}
        public Property<V> setPersistent(boolean f){persistent=f;return this;}
        public boolean isPersistent(){return persistent;}
        public V get(Config store,V def){
            return store.getProperty(this,def);
        }
        public V get(Config store){
            return get(store,initial);
        }
        public String getString(Config conf){
            V val=get(conf);
            return String.valueOf(Handy.normalize(String.class,val));
        }
        public Property<V> set(Config store,V val){
            store.setProperty(this, val);
            return this;
        }
        public void setString(Config store,String val){
            store.setProperty(this, adaptValue(val));
        }
        /** converts value such as string to expected type if possible. */
        public V adaptValue(Object val){
            val=Handy.normalize(this.getTyp(),val);
            return this.getTyp().cast(val);
        }
        @Override
        public int hashCode(){
            return name.toLowerCase().hashCode();
        }
        @Override
        public boolean equals(Object other){
            if(other==this) return true;
            if(other instanceof Property){
                Property<?> pother=(Property<?>) other;
                return name.equalsIgnoreCase(pother.getName()) && typ==pother.getTyp();
            }
            return false;
        }
    }
    public static abstract class Base implements Config {
        protected final HashMap<Property<?>,Object> props=new HashMap<>();
        final ArrayList<Property<?>> schema=new ArrayList<>(); 
        protected Object modified;
        public boolean isModified(){
            return modified!=null;
        }
        public Config setModified(Object p){
            if(p==null){
                // reset modified state
                modified=null;
            }else{
                if(modified==null) modified=new HashSet<Object>();
                if(modified instanceof Collection){
                    ((Collection)modified).add(p);
                }else{
                    // modified is set already but not appendable
                }
            }
            return this;
        }
        @Override
        public Config clear(){
            props.clear();
            modified=null;
            return this;
        }
        @Override
        public <T> Config setProperty(Property<T> key, T val) {
            //if(!key.isWritable()) throw new RuntimeException("read only property:"+key);
            setModified(key);
            props.put(key,val);
            return this;
        }
        @Override
        public <T> T delProperty(Property<T> key) {
            setModified(key);
            Object val=props.remove(key);
            return key.getTyp().cast(val);
        }
        @Override
        public Iterator<Property<?>> iterator() {
            ArrayList<Property<?>> keys=new ArrayList<>(props.keySet());
            return keys.iterator();
        }
        @Override
        public Config importSchema(boolean do_clear,Property<?> ...p){
            if(do_clear) this.schema.clear();
            for(Property<?> pp:p) schema.add(pp);
            return this;
        }
        @Override
        public Property<?>[] getSchema(){
            return schema.toArray(new Property<?>[schema.size()]);
        }
    }

    public static final Property<String> LOG_LEVEL=new Property<>("LOG_LEVEL",String.class);
    public static final Property<Logger> LOGGER=new Property<>("LOGGER",Logger.class);
    public static final Property<String> APP_INVOKED=new Property<>("APP_INVOKED",String.class);
    public static final Property<String> APP_NAME=new Property<>("APP_NAME",String.class);
    public static final Property<String> APP_TITLE=new Property<>("APP_TITLE",String.class);
    public static final Property<String> APP_INFO=new Property<>("APP_INFO",String.class);
    public static final Property<String> APP_WORKDIR=new Property<>("APP_WORKDIR",String.class);
    public static final Property<String> APP_SETTINGS=new Property<>("APP_SETTINGS",String.class);
    public static final Property<String> APP_CLASS=new Property<>("APP_CLASS",String.class);
    public static final Property<List> APP_ARGS=new Property<>("APP_ARGS",List.class);

    public default Config getParent(){return null;};
    public Config clear();
    public Config load() throws IOException;
    public Config save() throws IOException;
    public String getId();
    public <T> boolean hasProperty(Property<T> key);
    public <T> Config setProperty(Property<T> key,T val);
    public <T> T getProperty(Property<T> key,T def);
    public <T> T delProperty(Property<T> key);
    public Config importSchema(boolean clear,Property<?> ...p);
    public Property<?>[] getSchema();
}
