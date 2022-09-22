/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.util.HashMap;

import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.jabba.ui.Feedback;

public class AppSession implements Session{
    public static interface Factory{
        AppSession create(String id,App app);
    } 
    final String id;
    final App app;
    final HashMap<String,Object> values=new HashMap<>();
    long timeCreated;
    long lastActive;
    long maxAge;
    SecurityActor user;
    Feedback feedback;

    public AppSession(String id,App app){
        this.id=id;
        this.app=app;
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
    public App getApp(){
        return app;
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
    public static AppSession getInstance() {
        CallSession ss=CallSession.getInstance();
        return ss!=null?(AppSession)ss.getAppSession():null;
    }
    public Feedback getFeedback() {
        if(feedback==null) feedback=new Feedback();
        return feedback;
    }
}
