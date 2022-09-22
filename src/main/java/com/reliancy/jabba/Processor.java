/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Processor {
    protected Processor next;
    protected String id;
    protected boolean active;
    protected transient Config config;
    protected Logger logger;

    public Processor(String id){
        next=null;
        this.id=id!=null?id:this.getClass().getSimpleName().toLowerCase();
        active=true;
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
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Config getConfig() {
        return config;
    }
    /*
    public void setConfig(Config config) {
        this.config = config;
    }
    */
    /**
     * Main event processing chain.
     * Will go down the chain until result code is set.
     * @param request
     * @param response
     * @throws IOException
     */
    public void process(Request request,Response response) throws IOException {
        CallSession ss=CallSession.getInstance();
        try{
            ss.enter(this);
            if(!active){
                if(next!=null) next.process(request, response);
            }else{
                before(request, response);
                if(response.getStatus()==null) serve(request, response);
                if(next!=null && response.getStatus()==null) next.process(request, response);
                after(request, response);
            }
        }finally{
            ss.leave(this);
        }
    }
    public void begin(Config conf) throws Exception{
        this.config=conf;
    }
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
    public abstract void before(Request request,Response response) throws IOException;
    public abstract void after(Request request,Response response) throws IOException;
    public abstract void serve(Request request,Response response) throws IOException;
}
