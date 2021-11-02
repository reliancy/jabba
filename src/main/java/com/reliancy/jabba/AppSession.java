package com.reliancy.jabba;

import java.util.HashMap;

import com.reliancy.jabbasec.SecurityActor;

public class AppSession implements Session{
    final String id;
    final HashMap<String,Object> values;
    long timeCreated;
    long lastActive;
    long maxAge;
    SecurityActor user;
    
    public AppSession(String id){
        this.id=id;
        values=new HashMap<>();
        lastActive=timeCreated=System.currentTimeMillis();
        maxAge=1000*60*15;
    }
    @Override
    public void setValue(String key, Object val) {
        if(val!=null) values.put(key,val);
        else values.remove(key);
    }
    @Override
    public Object getValue(String key) {
        return values.get(key);
    }
    public long getTimeInactive(){
        return System.currentTimeMillis()-lastActive;
    }
    public void setLastActive(){
        lastActive=System.currentTimeMillis();
    }
    public boolean isExpired(){
        return getTimeInactive()>maxAge;
    }
    /** allows specialized appsessions to register with more ids. */
    protected void onPublish(HashMap<String,AppSession> directory){
    }
    /** allows specialized appsessions to deregister with more ids. */
    protected void onRetract(HashMap<String,AppSession> directory){
    }
    static HashMap<String,AppSession> instances=new HashMap<>();
    public static AppSession getInstance(String id){
        return instances.get(id);
    }
    public static void setInstance(String id,AppSession ss){
        AppSession old=getInstance(id);
        if(ss!=null){
            if(ss==old) return; // already published
            instances.put(id,ss);
            ss.onPublish(instances);
        }else{
            if(ss==old) return;  // already retracted
            old.onRetract(instances);
            instances.remove(id);
        }
    }
    public SecurityActor getUser() {
        return user;
    }
    public void setUser(SecurityActor user){
        this.user=user;
    }
}
