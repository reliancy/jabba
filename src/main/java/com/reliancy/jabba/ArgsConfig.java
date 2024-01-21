package com.reliancy.jabba;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.reliancy.util.Handy;
import com.reliancy.util.Log;

/**
 * ArgsConfig takes in command line arguments and parses them on load.
 * If also deals with environmental variables which are returned if not present.
 * Note that in java we have System properties which are platform independent 
 * then we have environment variables which are not unless fed by us directly.
 */
public class ArgsConfig extends Config.Base{
    final String[] args;
    String id;
    public ArgsConfig(String... args){
        this.args=args;
        this.id="";
    }
    @Override
    public Config load() throws IOException {
        if(props.isEmpty()==false) return this; // not gona load again if loaded
        final Map<String, List<String>> params = new HashMap<>();
        List<String> values=null;
        ArrayList<String> positional=new ArrayList<>();
        String app_invoked="";
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];
            if (a.charAt(0) == '-') {
                if(!positional.isEmpty()){
                    // positional filled - we are entering new level
                    throw new RuntimeException("multi level arguments not supported");
                }
                String key=a.substring(1);
                boolean brief=!key.startsWith("-");
                if(!brief) key=key.substring(1);
                if (key.length() < 1) {
                    throw new IllegalArgumentException("bad argument -");
                }
                values=new ArrayList<>();
                params.put(key,values);
                if(key.contains("=")){
                    // we got value after equals
                    String[] toks =key.split("=",1);
                    key=toks[0];
                    String val=toks[1];
                    val=Handy.unwrap(val,"\"","\"");
                    val=Handy.unwrap(val,"'","'");
                    values.add(val);
                }
            }else if (values != null) {
                //TODO: if property had a max count we could delay
                // this way multiple values require repeated --key val
                String val=a;
                val=Handy.unwrap(val,"\"","\"");
                val=Handy.unwrap(val,"'","'");
                values.add(a);
                values=null;
            }else {
                // these are positional arguments
                if(i==0) app_invoked=a; else positional.add(a);
            }
        }
        // now process parsed
        id=app_invoked;
        this.setProperty(Config.APP_INVOKED,app_invoked);
        int lastDelim=Math.max(app_invoked.lastIndexOf('/'),app_invoked.lastIndexOf('\\'));
        this.setProperty(Config.APP_NAME,app_invoked.substring(lastDelim+1));
        this.setProperty(Config.APP_ARGS,positional);
        //System.out.println("App invoked:"+app_invoked);
        //System.out.println("params:"+params);
        //System.out.println("pos:"+positional);
        for(String pkey:params.keySet()){
            values=params.get(pkey);
            if(values.isEmpty()){
                // we got a boolean
                Property<Boolean> prop=new Property<>(pkey,Boolean.class);
                setProperty(prop,true);
            }else if(values.size()==1){
                // we got single value
                String val=values.get(0);
                Property<?> prop=null;
                if("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)){
                    prop=new Property<>(pkey,Boolean.class);
                }else if(Handy.isNumeric(val)){
                    prop=new Property<>(pkey,Float.class);
                }else{
                    prop=new Property<>(pkey,String.class);
                }
                prop.setString(this, val);
            }else{
                // we got a list
                Property<List> prop=new Property<>(pkey,List.class);
                setProperty(prop,values);
            }
        }
        // finally post process such as choosing APP_WORKDIR
        String cwd=null;
        String[] cwds=new String[]{APP_WORKDIR.get(this),"./var","./data","../var","../data"};
        for(String c:cwds){
            if(c==null) continue;
            File f=new File(c);
            if(f.exists() && f.isDirectory()){
                cwd=c;break;
            }
        }
        if(cwd!=null){ // we got working dir
            cwd=cwd.replace("\\", "/").replace("/./","/");
            if(!cwd.endsWith("/")) cwd+="/";
            APP_WORKDIR.set(this, cwd);
        }
        String conf=null;
        String[] confs=new String[]{APP_SETTINGS.get(this),"./etc","./conf","./config","../etc","../conf","../config"};
        for(String c:confs){
            if(c==null) continue;
            File f=new File(c);
            if(f.exists() && f.isFile()){
                conf=c;break;
            }
        }
        if(conf!=null){ // we got settings file
            conf=conf.replace("\\", "/").replace("/./","/");
            APP_SETTINGS.set(this, cwd);
        }
        // also logging level and format
        // System.out.println("LogLog:"+LOG_LEVEL.get(this));
        // System.out.println("ENV:"+System.getenv("LOG_LEVEL"));
        // LOG_LEVEL.set(this,"INFO");
        Logger root=Log.setup();
        Log.setLevel(root,LOG_LEVEL.get(this));
        return this;
    }
    @Override
    public Config save() {
        return this;
    }
    @Override
    public String getId() {
        return id;
    }
    @Override
    public <T> boolean hasProperty(Property<T> key){
        String BADVAL="___BADVAL___";
        if(props.containsKey(key)){
            return true;
        }else if(System.getProperty(key.getName(),BADVAL)!=BADVAL){
            return true;
        }else if(System.getenv().containsKey(key.getName())){
            return true;
        }else{
            return false;
        }
    }
    @Override
    public <T> T getProperty(Property<T> key, T def) {
        String BADVAL="___BADVAL___";
        if(props.containsKey(key)){
            return key.getTyp().cast(props.get(key));
        }
        String ret=System.getProperty(key.getName(),BADVAL);
        if(ret!=BADVAL){
            Object val=Handy.normalize(key.getTyp(),ret);
            return key.getTyp().cast(val);
        }else if(System.getenv().containsKey(key.getName())){
            Object val=System.getenv(key.getName());
            val=Handy.normalize(key.getTyp(),val);
            return key.getTyp().cast(val);
        }else{
            return def;
        }
    }

   
}
