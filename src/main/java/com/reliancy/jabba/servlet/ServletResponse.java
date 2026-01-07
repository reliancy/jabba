/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.reliancy.jabba.HTTP;
import com.reliancy.jabba.Request;
import com.reliancy.jabba.Response;
import com.reliancy.jabba.ResponseState;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet-based implementation of Response.
 * Wraps HttpServletResponse to provide response functionality.
 */
public class ServletResponse extends Response {
    protected final HttpServletResponse http_response;

    public ServletResponse(Request request, HttpServletResponse http_response) {
        super(request);
        this.http_response = http_response;
    }
    @Override
    public OutputStream getOutputStream() throws IOException{
        if(http_response!=null) return http_response.getOutputStream();
        return byte_response;
    }
    @Override
    public Writer getWriter() throws IOException{
        if(http_response!=null) return http_response.getWriter();
        return char_response;
    }

    @Override
    public void setContentType(String ctype) {
        transitionTo(ResponseState.CONFIGURING);
        content_type=ctype;
        if(http_response!=null) http_response.setContentType(ctype);
    }

    @Override
    public void setStatus(int status) {
        transitionTo(ResponseState.CONFIGURING);
        this.status=status;
        if(http_response!=null) http_response.setStatus(status);
    }

    @Override
    public String getHeader(String key){
        for(HTTP.Header hdr:headers){
            if(key.equalsIgnoreCase(hdr.key)) return hdr.value;
        }
        if(http_response!=null){
            return http_response.getHeader(key);
        }else{
            return null;
        }
    }

    @Override
    public Response setHeader(String key, String val){
        transitionTo(ResponseState.CONFIGURING);
        if(!state.canConfigure()) {
            throw new IllegalStateException("Cannot set header in state: " + state);
        }
        HTTP.Header sel=null;
        for(HTTP.Header hdr:headers){
            if(key.equalsIgnoreCase(hdr.key)){
                sel=hdr;
                break;
            }
        }
        if(sel!=null) sel.value=val; else headers.add(new HTTP.Header(key,val));
        if(http_response!=null) http_response.setHeader(key,val);
        return this;
    }

    @Override
    public Response setCookie(String key, String val, int maxAge, boolean secure){
        return setCookie(key, val, maxAge, secure, true);
    }
    
    public Response setCookie(String key, String val, int maxAge, boolean secure, boolean httpOnly){
        transitionTo(ResponseState.CONFIGURING);
        if(!state.canConfigure()) {
            throw new IllegalStateException("Cannot set cookie in state: " + state);
        }
        HTTP.Cookie sel=null;
        for(HTTP.Cookie hdr:cookies){
            if(key.equalsIgnoreCase(hdr.key)){
                sel=hdr;
                break;
            }
        }
        if(sel!=null){
            sel.value=val;
            sel.maxAge=maxAge;
            sel.secure=secure;
            sel.httpOnly=httpOnly;
        } else{
            cookies.add(new HTTP.Cookie(key,val,maxAge,secure,httpOnly));
        }
        if(http_response!=null){
                Cookie c=new Cookie(key,val);
                c.setMaxAge(maxAge);
                c.setSecure(secure);
                c.setHttpOnly(httpOnly);
                http_response.addCookie(c);
        }
        return this;
    }

    @Override
    public boolean isCommitted(){
        return state.isCommitted();
    }

    @Override
    public void commit() {
        if(isCommitted()) return;
        if(getState() == ResponseState.CREATED || getState() == ResponseState.CONFIGURING){
            if(getStatus()==null) setStatus(Response.HTTP_OK);
            if(getContentType()==null) setContentType("text/plain;charset=utf-8");
            transitionTo(ResponseState.CONFIGURING);
        }
        if(http_response!=null && getState() == ResponseState.CONFIGURING){
            if(!http_response.isCommitted()){
                try {
                    http_response.flushBuffer();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to commit response", e);
                }
            }
        }
        transitionTo(ResponseState.COMMITTED);
    }

    @Override
    public boolean isCompleted(){
        return state.isCompleted();
    }

    @Override
    public void complete() {
        try {
            if(encoder!=null) encoder.flush();
            if(http_response!=null) http_response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("Failed to complete response", e);
        }
        transitionTo(ResponseState.COMPLETED);
        request.finish();
    }
    
    /**
     * Get the underlying HttpServletResponse.
     * @return the HttpServletResponse
     */
    public HttpServletResponse getHttpServletResponse(){
        return http_response;
    }
    
    /**
     * Upgrade HTTP response to WebSocket.
     * 
     * TODO: Implementation needed:
     * 1. Get HttpServletRequest from request (cast to ServletRequest)
     * 2. Get ServerContainer from ServletContext
     * 3. Create ServerEndpointConfig programmatically
     * 4. Call container.upgradeHttpToWebSocket(request, response, config, pathParams)
     * 5. Create ServletWebSocketSession wrapping Jakarta WebSocket Session
     * 6. Wire up message handlers to bridge Jakarta events to our callbacks
     * 
     * See: jakarta.websocket.server.ServerContainer
     * See: org.eclipse.jetty.ee10.websocket APIs
     */
    @Override
    public com.reliancy.jabba.WebSocketSession upgradeToWebSocket(String route, com.reliancy.jabba.Session appSession) throws IOException {
        return ServletWebSocketSession.create(this,route, appSession);
    }
}

