/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract base class of request/response handlers.
 * App is a processor and under it a router and a chain of filters are also processors.
 * Also endpoints are processors too.
 */
public abstract class Processor implements Servant {
    protected Processor parent;
    protected Processor next;
    protected String id;
    protected boolean active;
    protected transient Config config;
    protected Logger logger;
    protected boolean isAsync;

    public Processor(String id){
        next=null;
        this.id=id!=null?id:this.getClass().getSimpleName();
        active=true;
        isAsync=false;
    }
    public String getId(){
        return id;
    }
    
    public Processor getNext() {
        return next;
    }
    public void setNext(Processor next) {
        this.next = next;
    }
    /**
     * Find a processor of the given class type in the parent chain.
     * @param cls the class type to search for
     * @return the processor if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends Processor> T getParent(Class<T> cls) {
        Processor p=parent;
        while(p!=null){
            if(cls.isAssignableFrom(p.getClass())) return (T) p;
            p=p.getParent();
        }
        return null;
    }
    public Processor getParent() {
        return parent;
    }
    public void setParent(Processor p) {
        if(parent!=null && p!=null && p!=parent){
            throw new IllegalStateException("processor is attached to different parent");
        }
        this.parent = p;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public Config getConfig() {
        if(config!=null) return config;
        if(parent!=null) return parent.getConfig();
        return null;
    }
    /** Internal processing method that can handle async and non-async use cases.
     * Process the request and response, handling async if needed.
     * @param request
     * @param response
     * @param isAsync
     * @throws IOException
     */
    protected void process(Request request,Response response) throws IOException {
        final CallSession ss=CallSession.getInstance();
        // now we must account for async downstream
        final Processor thisProcessor=this;
        ss.enter(thisProcessor);
        if(!active){
            if(next!=null){
                next.process(request, response);
                return;
            }
        }else{
            beforeServe(request, response);
            serve(request, response);
            if(response.isPromised()==false){
                afterServe(request, response);
                ss.leave(thisProcessor);
            }else{
                response.promiseNext((value) -> {
                    try {
                        afterServe(request, response);
                        return value;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }finally{
                        ss.leave(thisProcessor);
                    }
                });
            }
        }
    }

    /** Place to prepare for a run. */
    public void begin(Config conf) throws Exception{
        this.config=conf;
    }
    /** Special null config begin only useful for middleware (to force them to use parent). */
    protected void begin() throws Exception {
        this.begin(null);
    }
    /**
     * cleans up by detaching from config.
     * Also notifies any waiting clients that it is done.
     * @throws Exception
     */
    public void end() throws Exception{
        this.config=null;
    }
    public void work() throws Exception{
    }
    protected Logger log(){
        // prefer local over central one 
        Logger ret=logger!=null?logger:(config!=null?Config.LOGGER.get(config):null);
        // if none provided install a fresh one locally
        if(ret==null) ret=logger=LoggerFactory.getLogger(this.getId());
        return ret;
    }
    /**
     * Check if this endpoint handles async requests.
     * @return true if method returns CompletableFuture
     */
    public boolean isAsync() {
        return isAsync;
    }
    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    /** called before serve. */
    public void beforeServe(Request request,Response response) throws IOException{

    }
    /** called after serve. */
    public void afterServe(Request request,Response response) throws IOException{

    }
    /** default implementation of work. 
     * if next processor is not null and response status is null, it will process the next processor.
     * otherwise it will return null if sync, or a completed future if async.
    */
    public void serve(Request request,Response response) throws IOException{
        if(next==null || response.getStatus()!=null) return;
        next.process(request, response);
    }
}
