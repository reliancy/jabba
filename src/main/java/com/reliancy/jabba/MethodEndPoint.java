/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import com.reliancy.jabba.decor.Async;
import com.reliancy.jabba.decor.Routed;
import com.reliancy.jabba.decor.WebSocket;
import com.reliancy.util.Handy;

public class MethodEndPoint extends EndPoint{
    // Inner Servant classes for each invoke type
    private final Servant INVOKE_PLAIN = new Servant() {
        @Override
        public void serve(Request request, Response response) throws IOException {
            try {
                method.invoke(target, request, response);
            } catch (Exception ex) {
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new IOException(ex);
            }
        }
    };
    
    private final Servant INVOKE_NOARG = new Servant() {
        @Override
        public void serve(Request request, Response response) throws IOException {
            try {
                Object ret = method.invoke(target);
                encodeResponse(ret, response);
            } catch (Exception ex) {
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new IOException(ex);
            }
        }
    };
    
    private final Servant INVOKE_FULL = new Servant() {
        @Override
        public void serve(Request request, Response response) throws IOException {
            try {
                Object[] argVals = decodeRequest(request);
                Object ret = method.invoke(target, argVals);
                encodeResponse(ret, response);
            } catch (Exception ex) {
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new IOException(ex);
            }
        }
    };
    
    private final Servant INVOKE_WEBSOCKET = new Servant() {
        @Override
        public void serve(Request request, Response response) throws IOException {
            try {
                // 1. Get AppSession from CallSession (set by middleware during upgrade request)
                CallSession cs = CallSession.getInstance();
                Session appSession = cs != null ? cs.getAppSession() : null;
                
                // 2. Get route path for this WebSocket endpoint
                String routePath = route != null ? route.path() : request.getPath();
                
                // 3. Upgrade HTTP response to WebSocket
                //    TODO: ServletResponse.upgradeToWebSocket() needs implementation
                WebSocketSession wsSession = response.upgradeToWebSocket(routePath, appSession);
                
                // 4. Invoke user method to setup callbacks
                //    User method signature: void methodName(WebSocketSession session)
                method.invoke(target, wsSession);
                
                // 5. Don't complete response - WebSocket connection stays open
                //    CallSession.end() will happen in finally block but WebSocketSession lives on
                //    TODO: Verify response handling - should we mark as async or handled differently?
                
            } catch (Exception ex) {
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new IOException(ex);
            }
        }
    };

    Routed route;
    Object target;
    Method method;
    Parameter[] params;
    Class<?> retType;
    Servant invokeType;
    ArrayList<MethodDecorator> decorators=new ArrayList<>();
    
    
    public MethodEndPoint(Object target,Method m) {
        super(target.getClass().getSimpleName()+"."+m.getName());
        this.route=m.getAnnotation(Routed.class);
        this.target=target;
        this.method=m;
        this.params=m.getParameters();
        this.retType=m.getReturnType();
        this.invokeType=INVOKE_FULL;
        if(params.length==2 && params[0].getType()==Request.class && params[1].getType()==Response.class){
            invokeType=INVOKE_PLAIN;
        }
        if(params.length==0){
            invokeType=INVOKE_NOARG;
        }
        // Check for WebSocket endpoint
        if(m.isAnnotationPresent(WebSocket.class)) {
            // WebSocket methods must have exactly one parameter of type WebSocketSession
            if(params.length != 1 || params[0].getType() != WebSocketSession.class) {
                throw new RuntimeException(
                    "@WebSocket method must have exactly one WebSocketSession parameter: " + 
                    m.getName()
                );
            }
            invokeType = INVOKE_WEBSOCKET;
            // TODO: WebSocket endpoints should probably always be async?
            // For now, let user control with @Async if needed
        }
        // Auto-detect async from @Async annotation OR CompletableFuture return type
        setAsync(m.getAnnotation(Async.class) != null || CompletableFuture.class.isAssignableFrom(retType));
        bindDecorators();
    }
    public String getVerb(){
        return route.verb();
    }
    public String getPath() {
        String ret=route.path();
        if(!ret.startsWith("/")) ret="/"+ret;
        ret=ret.replace("{method}",method.getName());
        return ret;
    }
    public Routed getRoute(){
        return route;
    }
    /** pulls in and adds decorator filters to this methodcall that are supported. */
    protected final void bindDecorators(){
        for(Annotation a:method.getAnnotations()){
            MethodDecorator d=MethodDecorator.query(this,a);
            if(d!=null) decorators.add(d);
        }
    }
    /** serves the request by invoking invokeType.serve(request, response).
     * this method will lift execution of invoketype into async task if desired and possible.
     * Methods returning CompletableFuture are handled by encodeResponse() to avoid double wrapping.
     */
    @Override
    public void serve(Request request, Response response) throws IOException{
        // Only lift to async if @Async annotation but NOT returning CompletableFuture
        // (CompletableFuture returns are handled by encodeResponse to avoid double wrapping)
        boolean needsAsyncWrapper = isAsync() && !CompletableFuture.class.isAssignableFrom(retType);
        
        if(needsAsyncWrapper && request.goAsync()) {
            // Start async promise chain for @Async annotated methods
            response.promiseFirst(v -> {
                try {
                    invokeType.serve(request, response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }else{
            // Sync execution or CompletableFuture return (async handled in encodeResponse)
            invokeType.serve(request, response);
        }
    }
    protected Object[] decodeRequest(Request request){
        Object[] argVals=new Object[params.length];
        for(int i=0;i<argVals.length;i++){
            Parameter p=params[i];
            Class<?> cls=p.getType();
            String byName=p.getName();
            String byPos="_arg"+i;
            Object val=request.getParam(byName,request.getParam(byPos,null)); // get by name or pos
            // Validate input before normalization
            val=validateInput(val,cls,byName);
            argVals[i]=Handy.normalize(cls,val);
        }
        return argVals;
    }
    /**
     * Validates input before processing to prevent injection attacks and malformed data.
     * @param val raw input value
     * @param expectedType expected type
     * @param paramName parameter name for error messages
     * @return validated value (may be modified or rejected)
     * @throws IllegalArgumentException if validation fails
     */
    protected Object validateInput(Object val, Class<?> expectedType, String paramName){
        if(val==null) return null;
        
        // String validation
        if(val instanceof String){
            String str=(String)val;
            // Limit string length to prevent DoS
            if(str.length()>100000){
                log().warn("Input parameter '{}' exceeds maximum length, truncated",paramName);
                str=str.substring(0,100000);
            }
            // For string types, return as-is (normalization will handle conversion)
            if(expectedType==String.class || expectedType==CharSequence.class){
                return str;
            }
        }
        
        // Array validation
        if(val instanceof String[]){
            String[] arr=(String[])val;
            if(arr.length>1000){
                log().warn("Input parameter '{}' array exceeds maximum size",paramName);
                throw new IllegalArgumentException("Array parameter '"+paramName+"' exceeds maximum size");
            }
            for(String s:arr){
                if(s!=null && s.length()>100000){
                    log().warn("Input parameter '{}' array element exceeds maximum length",paramName);
                    throw new IllegalArgumentException("Array element in '"+paramName+"' exceeds maximum length");
                }
            }
        }
        
        // Type validation - ensure value can be converted to expected type
        if(expectedType.isPrimitive() || Number.class.isAssignableFrom(expectedType) || 
           Boolean.class.isAssignableFrom(expectedType) || expectedType==Boolean.class){
            // These will be validated during normalization
            return val;
        }
        
        return val;
    }
    /** Encodes the response to the response encoder. 
     * We handle here future value as well by chaining.
    */
    protected void encodeResponse(Object ret, Response response) throws IOException{
        final Request request=response.getRequest();
        if(ret instanceof CompletableFuture){
            // Method returns a future - we turn async
            @SuppressWarnings("unchecked")
            CompletableFuture<Object> future = (CompletableFuture<Object>)ret;
            
            // Check if we can go async - we are not using isAsync here
            if(request.goAsync()) {
                // we can go async
                // Chain the future directly - NO BLOCKING!
                response.promiseFirst(v -> future)  // Returns the future, Response will flatten it
                    .promiseNext(result -> {
                        // Encode the result (recursive call, but result is not a future)
                        try {
                            encodeResponse(result, response);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });
            } else {
                // Blocking fallback - wait for future synchronously
                Object result = future.join();
                encodeResponse(result, response);
            }
            return;  // Important: exit after setting up async chain
        }
        if(ret instanceof Response){
            // we have a response return  - take its status and content type
            Response resp=(Response)ret;
            if(resp!=response){
                response.setStatus(resp.getStatus());
                response.setContentType(resp.getContentType());
                resp.exportContent(response.getEncoder());
            }
        }else{
            // we do not have a response but must set status, content type
            String ctype=route.return_mime();
            if(Handy.isBlank(ctype)) ctype=HTTP.guess_mime(ret);
            response.setContentType(ctype);
            // Set status to OK if not already set
            if(response.getStatus()==null){
                response.setStatus(Response.HTTP_OK);
            }
            if(ret!=null){
                response.getEncoder().writeObject(ret);
            }
        }
    }
}
