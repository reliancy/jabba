/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import org.slf4j.Logger;

public interface Config {
    public static class Property<V> {

        final String name;
        final Class<V> typ;
        public Property(String name,Class<V> typ){
            this.name=name;
            this.typ=typ;
        }
        public String getName(){return name;}
        public Class<V> getTyp(){return typ;}
        public V get(Config store,V def){
            return store.getProperty(this,def);
        }
        public V get(Config store){
            return get(store,null);
        }
        public void set(Config store,V val){
            store.setProperty(this, val);
        }
    }
    public static final Property<String> LOG_LEVEL=new Property<String>("LOG_LEVEL",String.class);
    public static final Property<Logger> LOGGER=new Property<Logger>("LOGGER",Logger.class);

    public void load();
    public void save();
    public String getId();
    public <T> Config setProperty(Property<T> key,T val);
    public <T> T getProperty(Property<T> key,T def);
}
