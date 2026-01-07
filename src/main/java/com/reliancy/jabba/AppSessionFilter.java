/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** AppSession middleware will inject an appsession object into callsession.
 * During each request,response we will if not alrady present extract a cookie or param
 * and based on it install an app wide sesson dictionary.
 */
public class AppSessionFilter extends Processor{
    /**special key to identify session cookie. */
    public static final String KEY_NAME="jbssid";
    AppSession.Factory factory;
    App app;
    public AppSessionFilter(App a) {
        this(a,null);
    }
    public AppSessionFilter(App a,AppSession.Factory f) {
        super(AppSessionFilter.class.getSimpleName());
        app=a;
        if(f==null) f=(id,app)->new AppSession(id, app);
        factory=f;
    }
    @Override
    public void beforeServe(Request request, Response response) throws IOException {
        String ssid=(String)request.getParam(KEY_NAME,null);
        if(ssid==null){
            UUID uuid = UUID.randomUUID();
            ssid=uuid.toString();
        }
        AppSession ss=AppSession.getInstance(ssid);
        if(ss!=null){
            if(ss.isExpired()){
                // this app sessin expired - create a new one
                ss=factory.create(ssid,app);
                AppSession.setInstance(ssid, ss);
            }else{
                // this session is good
                ss.setLastActive();
            }
        }else{
            // no session available
            ss=factory.create(ssid,app);
            AppSession.setInstance(ssid, ss);
        }
        CallSession css=CallSession.getInstance();
        css.setAppSession(ss);
    }
    @Override
    public void afterServe(Request request, Response response) throws IOException {
        CallSession css=CallSession.getInstance();
        AppSession ss=(AppSession) css.getAppSession();
        // Determine if request is HTTPS
        boolean isSecure="https".equalsIgnoreCase(request.getProtocol()) || 
                         "https".equalsIgnoreCase(request.getScheme());
        // Set secure=true for HTTPS, HttpOnly=true always for security
        response.setCookie(KEY_NAME,ss.id,15*60,isSecure,true);
    }
}
