/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executor;

/**
 * Thread local object that lets us access some variables in specialized handler methods.
 * For example request and response objects are accessible.
 * The session is updated at process phase of each processor.
 * 
 * <h3>Instance Counting and Multi-Threading</h3>
 * CallSession tracks how many threads are currently using it via an atomic counter.
 * This enables safe async processing where a single session is shared across multiple threads:
 * 
 * <ul>
 * <li><b>beginFresh()</b> - Initialize session at the top of request processing (main thread)</li>
 * <li><b>beginAgain()</b> - Reattach session when switching threads (async workers)</li>
 * <li><b>end()</b> - Detach from current thread, decrement counter. Only cleans up when count reaches zero.</li>
 * </ul>
 * 
 * <p><b>Async Flow Example:</b></p>
 * <pre>
 * // Main thread:
 * session.beginFresh(appSession, request, response);  // count = 1
 * // ... processing ...
 * 
 * // Fork to async thread:
 * CompletableFuture.supplyAsync(() -> {
 *     session.beginAgain();   // count = 2
 *     // ... async work ...
 *     session.end();          // count = 1, session still alive
 * });
 * 
 * // Main thread completes:
 * session.end();              // count = 0, session cleanup happens
 * </pre>
 * 
 * <p>This ensures the session and its resources remain valid until ALL threads complete.</p>
 */
public class CallSession implements Session{
    ArrayList<Processor> callers=new ArrayList<>();
    Session appSession;
    Request request;
    Response response;
    Executor executor;
    /** Atomic counter tracking how many threads are currently using this session */
    transient AtomicInteger instanceCount=new AtomicInteger(0);

    public CallSession(){
    }
    /** End the current session. 
     * If the session is not the current one, do nothing.
     * If the session is the current one, remove it from the thread local.
     * If the session is the current one and there are no more instances, clear the session.
     * If the session is the current one and there are more instances, decrement the instance count.
    */
    public synchronized boolean end(){
        CallSession current=instance.get();
        if(current!=this) return false; // not the current session
        instance.remove(); // remove from this thread
        int count=instanceCount.updateAndGet(i -> i>0 ? i-1 : 0);
        if(count==0){
            // if no more instances, clear the session
            try{
                while(callers.size()>0){
                    Processor last=callers.remove(callers.size()-1);
                    if(last!=null && last.isActive()){
                        try{
                            last.afterServe(request, response); // call after to ensure proper cleanup
                        }catch(Exception e){
                            // Log but don't throw - we're in cleanup
                            last.log().error("Error calling after() on processor " + last.getId() + ": " + e.getMessage());
                        }
                    }
                };
            }finally{
                appSession=null;
                request=null;
                response=null;
                executor=null;
                callers.clear();
            }
            return true;
        }
        return false;
    }
    /** Begins session at the top of the call stack.
     * If the session is already in use, throw an exception.
     * If the session is not in use, set the session to the new one.
     * @param ss
     * @param req
     * @param resp
     * @return true if the session was successfully begun, false otherwise
     */
    public synchronized boolean beginFresh(Session ss,Request req,Response resp){
        appSession=ss;
        request=req;
        response=resp;
        executor=null;
        callers.clear();
        return beginAgain();
    }
    
    /** Begins session again in a different thread..
     * @return true if the session was successfully begun, false otherwise
     */
    public synchronized boolean beginAgain(){
        CallSession current=instance.get();
        if(this==current) return true;
        if(current!=null) current.end(); // end previous one if any
        instance.set(this); // add to this thread
        instanceCount.incrementAndGet(); // increment count
        return true;
    }
    protected void enter(Processor c){callers.add(c);}
    protected void leave(Processor c){
        int len=callers.size();
        int at=len-1;
        Processor last=len>0?callers.get(at):null;
        if(c!=null && c==last){
            callers.remove(len-1);
        }else if(len>0 && (at=callers.indexOf(c))!=-1){
            // bad last is not same c, some processors have not left properly
            do{
                last=callers.remove(callers.size()-1);
                if(last!=null && last.isActive()){
                    try{
                        last.afterServe(request, response); // call after to ensure proper cleanup
                    }catch(Exception e){
                        // Log but don't throw - we're in cleanup
                        last.log().error("Error calling after() on processor " + last.getId() + ": " + e.getMessage());
                    }
                }
            }while(last!=c);
        }
    }
    @Override
    public void setValue(String key, Object val) {
        if(appSession!=null) appSession.setValue(key, val);
    }
    @Override
    public Object getValue(String key) {
        return appSession!=null?appSession.getValue(key):null;
    }
    public void setAppSession(Session ss) {
        appSession=ss;
    }
    public Session getAppSession() {
        return appSession;
    }
    public Request getRequest() {
        return request;
    }
    public void setRequest(Request request) {
        this.request = request;
    }
    public Response getResponse() {
        return response;
    }
    public void setResponse(Response response) {
        this.response = response;
    }
    public Executor getExecutor() {
        return executor;
    }
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
    public Processor getCaller() {
        int len=callers.size();
        return len>0?callers.get(len-1):null;
    }
    public static ThreadLocal<CallSession> instance=new ThreadLocal<>();
    /**
     * Will return current session given the call stack.
     * @return thread local call session
     */
    public static CallSession getInstance(){
        CallSession ret=instance.get();
        if(ret==null) instance.set(ret=new CallSession());
        return ret;
    }
    /** Set the current call session. 
     * If the session is the same as the current one, do nothing.
     * If the session is null, end the current one if any.
     * If the session is new, end the current one if any and set the new one.
    */
    // public static void setInstance(CallSession ss){
    //     CallSession current=instance.get();
    //     if(ss==current) return;
    //     if(current!=null) current.end(); // end previous one if any
    //     if(ss!=null){
    //         instance.set(ss); // add to this thread
    //         ss.instanceCount.incrementAndGet(); // increment count
    //     }
    // }
}
