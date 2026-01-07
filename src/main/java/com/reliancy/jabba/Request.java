/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract representation of an HTTP request.
 * Provides container-agnostic access to request properties.
 */
public abstract class Request {
    protected final HashMap<String,String> pathParams=new HashMap<>();
    protected String pathOverride;
    protected Runnable finisher;
    protected CallSession session;

    public Request() {
        finisher = () -> {};
    }
    public CallSession getSession() {
        return session;
    }
    
    public void setSession(CallSession session) {
        this.session = session;
    }

    public void setFinisher(Runnable finisher) {
        this.finisher = finisher;
    }

    public boolean isFinished() {
        return finisher == null;
    }
    
    public abstract void finish();
    
    public abstract boolean isAsync();
    public abstract boolean goAsync();
    
    public Map<String,String> getPathParams(){
        return pathParams;
    }
    public Request setPath(String path){
        pathOverride=path;
        return this;
    }
    
    public abstract String getPath();
    
    
    public abstract String getVerb();
    
    public abstract Object getParam(String pname, Object def);
    
    public abstract Request setParam(String pname, Object val);
    
    public abstract String getHeader(String key);
    
    public abstract String getCookie(String name, String def);
    
    public abstract String getRemoteAddress();
    
    public abstract String getMount();
    
    public abstract String getProtocol();
    public abstract String getScheme();
    
}
