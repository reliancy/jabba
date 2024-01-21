/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.util.HashMap;
import java.util.Map;

import com.reliancy.util.Handy;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class Request {
    final HttpServletRequest  http_request;
    final HashMap<String,String> pathParams=new HashMap<>();
    String pathOverride;
    public Request(HttpServletRequest http_request) {
        this.http_request = http_request;
    }
    public Map<String,String> getPathParams(){
        return pathParams;
    }
    public String getPath() {
        if(pathOverride!=null){
            return pathOverride;
        }else{
            return http_request.getPathInfo();
        }
    }
    public Request setPath(String path){
        pathOverride=path;
        return this;
    }
    public String getVerb() {
        return http_request.getMethod();
    }
    /**
     * Look for this parameter in pathParam, queryParams and forms.
     * @param pname
     * @return
     */
    public Object getParam(String pname,Object def){
        if(pathParams.containsKey(pname)) return pathParams.get(pname);
        String[] vals=http_request.getParameterValues(pname);
        if(vals!=null) return vals.length==1?vals[0]:vals;
        String hdr=getHeader(pname);
        if(hdr!=null) return hdr;
        String cook=getCookie(pname,null);
        if(cook!=null) return cook;
        return def;
    }
    public Request setParam(String pname,Object val){
        if(pathParams.containsKey(pname)){
            pathParams.put(pname,String.valueOf(Handy.nz(val,"")));
        }else{
            throw new IllegalArgumentException("invalid param name:"+pname);
        }
        return this;
    }
    public String getHeader(String key){
        return http_request.getHeader(key);
    }
    public String getCookie(String name,String def){
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
    /** 
     * This method will consult several headers to obain ip address.
     * @return best guess for remote address.
     */
    public String getRemoteAddress() {
        for (String header : HEADERS4IP) {
            String ip = getHeader(header);
            if(ip==null || ip.length()==0 || "unknown".equalsIgnoreCase(ip)) continue;
            return ip.contains(",")?ip.split(",",2)[0]:ip;
        }
        return http_request.getRemoteAddr();
    }
    /**
     * will return shema://host:port/context
     * @return everything preceeding the path.
     */
    public String getMount(){
        String scheme = http_request.getScheme();
        String host = http_request.getHeader("Host");        // includes server name and server port
        if(host==null || host.trim().isEmpty()){
            // try differenty for host
            String serverName = http_request.getServerName();
            int serverPort = http_request.getServerPort();
            host=serverName+":"+serverPort;
        }
        String resultPath = scheme + "://" + host;
        String contextPath = http_request.getContextPath();       // includes leading forward slash
        if(contextPath!=null){
            resultPath+= contextPath;
        }
        return resultPath;
    }
    public String getProtocol(){
        return http_request.getProtocol();
    }
}
