/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reliancy.util.Handy;

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
        if(path.endsWith(".ini")){
        // load ini
		try(BufferedReader reader= new BufferedReader(new FileReader(path))) {
            String header=null;
			for(String line = reader.readLine();line!=null;line=reader.readLine()){
                line=line.trim();
                if(line.isEmpty() || line.startsWith("#")) continue;
                if(line.startsWith("[") && line.endsWith("]")){
                    // this is a header
                    header=line.substring(1,line.length()-1);
                }else{
                    String[] kv=Handy.split("=", line,1);
                    String key=kv[0];
                    String val=kv[1];
                    if(header!=null) key=header+"_"+key;
                    Property<?> prop=findPropertyDef(key);
                    if(prop!=null){
                    }else if("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)){
                        prop=new Property<>(key,Boolean.class);
                    }else if(Handy.isNumeric(val)){
                        prop=new Property<>(key,Float.class);
                    }else{
                        prop=new Property<>(key,String.class);
                    }
                    prop.setString(this, val);
                    prop.setPersistent(true);
                }
			}
		}            
        }
        // now do some evaluations
        boolean changing=false;
        int iterations=0; // to prevent recursion
        do{
            iterations+=1;
            changing=false;
            for(Property<?> p:this){
                Object val=p.get(this);
                if(!(val instanceof String)) continue; // skip not a string
                String sval=String.valueOf(val);
                if(!sval.contains("${")) continue; // no variables used
                Pattern pat = java.util.regex.Pattern.compile("\\$\\{(.+?)\\}");
                Matcher mat = pat.matcher(sval);
                while(mat.find()){ // iterateo over matches inject other properties
                    Property<?> pp=findProperty(mat.group(1));
                    if(pp==null) continue;
                    sval=sval.replace(mat.group(0),pp.getString(this));
                    changing=true;
                }
                if(changing) p.setString(this,sval);
            }
        }while(changing && iterations<7);
        return this;
    }
    @Override
    public Config save() throws IOException{
        return this;
    }
    protected Property<?> findProperty(String name){
        Config cur=this;
        while(cur!=null){
            for(Property<?> p:cur){
                if(name.equalsIgnoreCase(p.getName())) return p;
            }
            cur=cur.getParent();
        }
        return null;
    }
    /** tries to locate properties in schema and static vars. */
    protected Property<?> findPropertyStat(String name,Object e){
        if(e==null) return null;
        if(!(e instanceof Class)) e=e.getClass();
        Class<?> c=(Class<?>)e;
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (Property.class.isAssignableFrom(field.getType())) {
                    Property<?> p=(Property<?>)field.get(c);
                    if(name.equalsIgnoreCase(p.getName())) return p;
                }
            }
            catch (IllegalAccessException xe) {
                // Handle exception here
            }
        }    
        for(Class<?> cint:c.getInterfaces()){
            Property<?> p=findPropertyStat(name,cint);
            if(p!=null) return p;
        }        
        return findPropertyStat(name,c.getSuperclass());
    }
    protected Property<?> findPropertyDef(String name,Object...ext){
        // first search over ext (objects and classes and properties)
        for(Object e:ext){
            if(e instanceof Property) if(((Property<?>)e).getName().equalsIgnoreCase(name)) return (Property<?>)e;
            Property<?> s=findPropertyStat(name,e);
            if(s!=null) return s;
        }
        Config cur=this;
        while(cur!=null){
            // lookup schema
            Property<?>[] sch=cur.getSchema();
            for(Property<?> p:sch) if(p.getName().equalsIgnoreCase(name)) return p;
            // lookup static
            Property<?> s=findPropertyStat(name,cur);
            if(s!=null) return s;
            cur=cur.getParent();
        }
        return null;
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
     * if name is uppercase defers to parent first (it could be argsconfig) then returns own.
     * else tries own first then checks parent.
     */
    @Override
    public <T> T getProperty(Config.Property<T> key, T def) {
        String key_name=key.getName();
        if(key_name.equals(key_name.toUpperCase())){
            if(parent!=null && parent.hasProperty(key)){
                return parent.getProperty(key, def);
            }else if(props.containsKey(key)){
                return key.getTyp().cast(props.get(key));
            }
        }else{
            if(props.containsKey(key)){
                return key.getTyp().cast(props.get(key));
            }else if(parent!=null && parent.hasProperty(key)){
                return parent.getProperty(key, def);
            }
        }
        return def;
    }

    
}
