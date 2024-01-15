/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;

public class FileConfig extends Config.Base{
    final Config parent;
    String path;
    public FileConfig(Config parent,String p){
        this.parent=parent;
        path=p;
    }
    public FileConfig(String p){
        this(null,p);
    }
    @Override
    public String getId() {
        return path;
    }
    @Override
    public Config getParent(){
        return parent;
    };
    @Override
    public Config load() throws IOException{
        if(parent!=null) parent.load();
        if(props.isEmpty()==false) return this; // not gona load again if loaded
        return this;
    }
    @Override
    public Config save() throws IOException{
        return this;
    }

    public FileConfig setId(String path) {
        this.path = path;
        return this;
    }
    @Override
    public <T> boolean hasProperty(Property<T> key){
        if(props.containsKey(key)){
            return true;
        }else if(parent!=null && parent.hasProperty(key)){
            return true;
        }else{
            return false;
        }
    }
    /**
     * FileConfig defers to parent first (it could be argsconfig) then returns own.
     */
    @Override
    public <T> T getProperty(Config.Property<T> key, T def) {
        if(parent!=null && parent.hasProperty(key)){
            return parent.getProperty(key, def);
        }else if(props.containsKey(key)){
            return key.getTyp().cast(props.get(key));
        }else{
            return def;
        }
    }

    
}
