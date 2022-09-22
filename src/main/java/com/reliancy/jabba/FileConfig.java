/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.util.HashMap;

public class FileConfig implements Config{
    String path;
    final HashMap<String,Object> props=new HashMap<>();
    public FileConfig(String p){
        path=p;
        load();
    }
    public FileConfig(){
        this(null);
    }
    public void clear(){
        props.clear();
    }
    @Override
    public void load() {
        
    }
    @Override
    public void save() {
    }

    @Override
    public String getId() {
        return path;
    }

    public void setId(String path) {
        this.path = path;
    }
    @Override
    public <T> T getProperty(Config.Property<T> key, T def) {
        if(props.containsKey(key.getName())) return key.getTyp().cast(props.get(key.getName()));
        else return def;
    }

    @Override
    public <T> Config setProperty(Config.Property<T> key, T val) {
        props.put(key.getName(),val);
        return this;
    }

    
}
