/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.jabba;

import java.io.IOException;

import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.util.CodeException;
import com.reliancy.util.ResultCode;

/** Base Application class from where specific launchers derive.
 * Derived classes will usually bring in jetty or tomcat or some other launch ability.
 */
public abstract class App extends Processor{
    public static int ERR_NOCONFIG=ResultCode.defineFailure(0x01,App.class,"config missing. provide at least empty one.");
    public static int ERR_NOTCLOSED=ResultCode.defineFailure(0x02,App.class,"unbalanced call. resource called twice:${resource}");
    protected Processor first=null;
    protected Processor last=null;
    protected RoutedEndPoint router=null;
    protected SecurityPolicy policy=null;

    public App(String id) {
        super(id);
    }
    public void before(Request request,Response response) throws IOException{
    }
    public void after(Request request,Response response) throws IOException{
    }
    public void serve(Request req,Response resp) throws IOException{
        if(first!=null) first.process(req, resp);
        if(router!=null) router.process(req,resp);
    }
    public <T extends Processor> T addProcessor(T m){
        if(first==null){
            last=first=m;
        }else{
            last.next=m;
        }
        while(last.next!=null) last=last.next;
        return m;
    }
    public void removeProcessor(Processor m){
        if(first==m){
            if(first==last) last=null;
            first=first.next;
            while(last!=null && last.next!=null) last=last.next;
        }else{
            for(Processor prev=first;prev!=null;prev=prev.next){
                if(prev.next==m){
                    if(last==m) last=prev;
                    prev.next=m.next;
                    break;
                }
            }
        }
        m.next=null;
    }
    public Processor getProcessor(String id){
        for(Processor c=first;c!=null;c=c.next){
            if(c.getId().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
    
    public RoutedEndPoint getRouter() {
        return router;
    }
    public void setRouter(RoutedEndPoint router) {
        this.router = router;
    }
    public void run(Config conf) throws Exception {
        try{
            begin(conf);
            work();
        }finally{
            end();
        }
    }
    @Override
    public void begin(Config conf) throws Exception{
        if(config!=null) throw new CodeException(ERR_NOTCLOSED).put("resource","Router.begin()");
        if(conf==null) throw new CodeException(ERR_NOCONFIG);
        config=conf;
        for(Processor p=first;p!=null;p=p.getNext()){
            p.begin(config);
        }
        if(router!=null) router.begin(config);
    }
    @Override
    public void end() throws Exception{
        if(router!=null) router.end();
        for(Processor p=first;p!=null;p=p.getNext()){
            p.end();
        }
        super.end();
        log().info("stopping app:"+getId());
    }
    public AppSessionFilter addAppSession(){
        return addProcessor(new AppSessionFilter(this));
    }
    public AppSessionFilter addAppSession(AppSession.Factory f){
        return addProcessor(new AppSessionFilter(this,f));
    }
    public SecurityPolicy setSecurityPolicy(SecurityPolicy secpol){
        if(secpol==policy) return secpol;
        if(policy!=null){
            MethodDecorator.retract(policy);
            removeProcessor(policy);
        }
        policy=secpol;
        if(policy!=null){
            addProcessor(policy);
            MethodDecorator.publish(policy); // register security policy as decorator factory
        }
        return secpol;
    }
    public SecurityPolicy getSecurityPolicy(){
        return policy;
    }
}
