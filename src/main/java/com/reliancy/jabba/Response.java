/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Abstract representation of an HTTP response.
 * Provides container-agnostic response handling with async support.
 */
public abstract class Response {
    // HTTP status codes
    public static final int HTTP_OK=200;
    public static final int HTTP_BAD_REQUEST=400;
    public static final int HTTP_NOT_FOUND=404;
    public static final int HTTP_UNAUTHORIZED=401;
    public static final int HTTP_FORBIDDEN=403;
    public static final int HTTP_TEMPORARY_REDIRECT=307;
    public static final int HTTP_FOUND_REDIRECT=302;
    public static final int HTTP_NOT_MODIFIED=304;
    public static final int HTTP_INTERNAL_ERROR=500;
    
    protected final Request request;
    protected final Writer char_response;
    protected final OutputStream byte_response;
    protected ResponseEncoder encoder;
    protected String content_type;
    protected Integer status;
    protected ResponseState state = ResponseState.CREATED;
    protected final ArrayList<HTTP.Header> headers=new ArrayList<>();
    protected final ArrayList<HTTP.Cookie> cookies=new ArrayList<>();
    protected CompletableFuture<Object> promise;

    protected Response(Request request) {
        this.request = request;
        this.char_response = null;
        this.byte_response = null;
    }
    
    protected Response(Writer w) {
        this.request = null;
        this.char_response=w;
        this.byte_response=null;
    }
    
    protected Response(OutputStream w) {
        this.request = null;
        this.char_response=null;
        this.byte_response=w;
    }
    
    protected Response() {
        this.request = null;
        this.char_response=new StringWriter();
        this.byte_response=null;
    }
    
    public ResponseState getState() {
        return state;
    }
    
    public void transitionTo(ResponseState newState) {
        this.state = this.state.transitionTo(newState);
    }
    
    public Request getRequest() {
        return request;
    }
    
    public ResponseEncoder getEncoder(){
        if(encoder==null) encoder=new ResponseEncoder(this);
        return encoder;
    }
    
    public Object getContent(){
        if(char_response instanceof StringWriter){
            return ((StringWriter)char_response).toString();
        }else if( byte_response instanceof ByteArrayOutputStream){
            return ((ByteArrayOutputStream)byte_response).toByteArray();
        }else return null;
    }
    
    public void exportContent(ResponseEncoder ext) throws IOException {
        if(char_response instanceof StringWriter){
            ext.writeString(((StringWriter)char_response).toString());
        }else if( byte_response instanceof ByteArrayOutputStream){
            byte[] buf=((ByteArrayOutputStream)byte_response).toByteArray();
            ext.writeBytes(buf,0,buf.length);
        }
    }
    public OutputStream getOutputStream() throws IOException{
        return byte_response;
    }
    public Writer getWriter() throws IOException{
        return char_response;
    }
    public abstract void setContentType(String ctype);
    
    public String getContentType(){
        return content_type;
    }
    
    public abstract void setStatus(int status);
    
    public Integer getStatus(){
        return status;
    }
    
    public abstract String getHeader(String key);
    
    public abstract Response setHeader(String key, String val);
    
    public List<HTTP.Header> getHeaders(){
        return headers;
    }
    
    public String getCookie(String key){
        for(HTTP.Cookie c:cookies){
            if(key.equalsIgnoreCase(c.key)) return c.value;
        }
        return null;
    }
    
    public abstract Response setCookie(String key, String val, int maxAge, boolean secure);
    
    public Response setCookie(String key, String val, int maxAge, boolean secure, boolean httpOnly){
        return setCookie(key, val, maxAge, secure);
    }
    
    public List<HTTP.Cookie> getCookies(){
        return cookies;
    }
    
    public abstract boolean isCommitted();
    
    public abstract void commit();
    
    public abstract boolean isCompleted();
    
    public abstract void complete();
    
    public boolean isPromised() {
        return promise!=null;
    }
    
    /**
     * Initiate an async promise chain using supplyAsync.
     * Gets executor from request's CallSession.
     * Automatically attaches/detaches CallSession for the executing thread.
     * Can be called multiple times - will chain after existing promise.
     * Automatically flattens if supplier returns a CompletableFuture.
     * @param supplier lambda that accepts one value and returns one value (or CompletableFuture)
     * @return this Response for chaining
     */
    public Response promiseFirst(Function<Object, Object> supplier) {
        if(request == null) {
            throw new IllegalStateException("Cannot create promise without request");
        }
        final CallSession session = request.getSession();
        Executor executorTemp = session != null ? session.getExecutor() : null;
        if(executorTemp == null) {
            executorTemp = java.util.concurrent.ForkJoinPool.commonPool();
        }
        final Executor executor = executorTemp;
        
        Function<Object, CompletableFuture<Object>> newTask = (prevValue) -> {
            CompletableFuture<Object> innerFuture = CompletableFuture.supplyAsync(() -> {
                session.beginAgain();
                try {
                    return supplier.apply(prevValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    session.end();
                }
            }, executor);
            
            return innerFuture.thenCompose(result -> {
                if(result instanceof CompletableFuture) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Object> futureResult = (CompletableFuture<Object>)result;
                    return futureResult;
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            });
        };
        
        if(promise != null) {
            promise = promise.thenCompose(newTask);
        } else {
            promise = newTask.apply(null);
        }
        return this;
    }
    
    /**
     * Add a step to the promise chain.
     * Automatically attaches/detaches CallSession for the executing thread.
     * @param step lambda that accepts the value from previous step and returns a value
     * @return this Response for chaining
     */
    public Response promiseNext(Function<Object, Object> step) {
        if(promise == null) {
            throw new IllegalStateException("Promise chain not initiated. Call promiseFirst() first.");
        }
        final CallSession session = request.getSession();
        promise = promise.thenApply(value -> {
            session.beginAgain();
            try {
                return step.apply(value);
            } catch (Exception e) {
                throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
            } finally {
                session.end();
            }
        });
        return this;
    }
    
    /**
     * Final step in the promise chain - finalizes response and handles errors.
     * Automatically attaches/detaches CallSession for the executing thread.
     * @param callback BiConsumer that receives result and error
     * @return this Response for chaining
     */
    public Response promiseLast(BiConsumer<Object, Throwable> callback) {
        if(promise == null) {
            throw new IllegalStateException("Promise chain not initiated. Call promiseFirst() first.");
        }
        final CallSession session = request.getSession();
        promise = promise.whenComplete((result, error) -> {
            session.beginAgain();
            try {
                callback.accept(result, error);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                session.end();
            }
        }).thenApply(v -> null);
        return this;
    }
    
    /**
     * Upgrade HTTP response to WebSocket.
     * Called by INVOKE_WEBSOCKET in MethodEndPoint.
     * 
     * @param route The WebSocket route path
     * @param appSession User session from CallSession (can be null)
     * @return WebSocketSession for this connection
     * @throws IOException if upgrade fails
     * 
     * TODO: Implement in ServletResponse using Jakarta WebSocket API
     */
    public abstract WebSocketSession upgradeToWebSocket(String route, Session appSession) throws IOException;
}
