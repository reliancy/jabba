/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.servlet;

import com.reliancy.jabba.Request;
import com.reliancy.util.Handy;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet-based implementation of Request.
 * Wraps HttpServletRequest to provide request functionality.
 */
public class ServletRequest extends Request {
    protected final HttpServletRequest http_request;
    protected AsyncContext asyncContext;

    public ServletRequest(HttpServletRequest http_request) {
        super();
        this.http_request = http_request;
    }

    @Override
    public void finish() {
        if(finisher != null){
            finisher.run();
            finisher = null;
        }
        if(asyncContext != null){
            asyncContext.complete();
            asyncContext = null;
        }
    }
    
    @Override
    public boolean isAsync() {
        return asyncContext != null;
    }
    
    /**
     * Start asynchronous processing if supported.
     * @return true if async is supported and started, false otherwise
     */
    public boolean goAsync() {
        if(asyncContext == null && http_request.isAsyncSupported()){
            asyncContext = http_request.startAsync();
            return true;
        }
        return false;
    }
    
    @Override
    public String getPath() {
        if(pathOverride!=null){
            return pathOverride;
        }else{
            return http_request.getPathInfo();
        }
    }
    
    @Override
    public String getVerb() {
        return http_request.getMethod();
    }
    
    @Override
    public Object getParam(String pname, Object def){
        if(pathParams.containsKey(pname)) {
            Object val = pathParams.get(pname);
            return val;
        }
        String[] vals=http_request.getParameterValues(pname);
        if(vals!=null) {
            Object result = vals.length==1?vals[0]:vals;
            return result;
        }
        String hdr=getHeader(pname);
        if(hdr!=null) return hdr;
        String cook=getCookie(pname,null);
        if(cook!=null) return cook;
        return def;
    }
    
    @Override
    public Request setParam(String pname, Object val){
        if(pathParams.containsKey(pname)){
            pathParams.put(pname,String.valueOf(Handy.nz(val,"")));
        }else{
            throw new IllegalArgumentException("invalid param name:"+pname);
        }
        return this;
    }
    
    @Override
    public String getHeader(String key){
        return http_request.getHeader(key);
    }
    
    @Override
    public String getCookie(String name, String def){
        Cookie[] all=http_request.getCookies();
        if(all!=null) for(Cookie c:all){
            if(name.equalsIgnoreCase(c.getName())) return c.getValue();
        }
        return def;
    }
    
    private static final String[] HEADERS4IP = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR" };
    
    @Override
    public String getRemoteAddress() {
        for (String header : HEADERS4IP) {
            String ip = getHeader(header);
            if(ip==null || ip.length()==0 || "unknown".equalsIgnoreCase(ip)) continue;
            return ip.contains(",")?ip.split(",",2)[0]:ip;
        }
        return http_request.getRemoteAddr();
    }
    
    @Override
    public String getMount(){
        String scheme = http_request.getScheme();
        String host = http_request.getHeader("Host");
        if(host==null || host.trim().isEmpty()){
            String serverName = http_request.getServerName();
            int serverPort = http_request.getServerPort();
            host=serverName+":"+serverPort;
        }
        String resultPath = scheme + "://" + host;
        String contextPath = http_request.getContextPath();
        if(contextPath!=null){
            resultPath+= contextPath;
        }
        return resultPath;
    }
    
    @Override
    public String getProtocol(){
        return http_request.getProtocol();
    }
    
    @Override
    public String getScheme(){
        return http_request.getScheme();
    }
    
    /**
     * Get the underlying HttpServletRequest.
     * @return the HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest(){
        return http_request;
    }
}

