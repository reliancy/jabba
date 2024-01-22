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

import com.reliancy.dbo.Terminal;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.ui.Rendering;
import com.reliancy.jabba.ui.Template;
import com.reliancy.util.CodeException;
import com.reliancy.util.ResultCode;

/** Base Application class from where specific launchers derive.
 * Derived classes will usually bring in jetty or tomcat or some other launch ability.
 * At the level of App we manage pure app or infrastructure concepts. 
 * Examples of such concepts are:
 *  - processor chain
 *  - main / router processor
 *  - security policy
 *  - storage which pools resources among instances
 *  - app wide logger and config
 * It does not include:
 *  - per user config
 *  - per user work directory
 * 
 * Storage is an abstract, possibly external data store. It could be file based or not.
 * On the other hand work path is usually local disk path specific to a machine, still
 * it can be overriden to return www resource paths for certain items. That is why we treat
 * it as a string and not a File or URL.
 */
public abstract class App extends Processor{
    public static int ERR_NOCONFIG=ResultCode.defineFailure(0x01,App.class,"config missing. provide at least empty one.");
    public static int ERR_NOTCLOSED=ResultCode.defineFailure(0x02,App.class,"unbalanced call. resource called twice:${resource}");
    protected Processor first=null;
    protected Processor last=null;
    protected Router router=null;
    protected SecurityPolicy policy=null;
    protected Terminal storage=null;
    protected Map<String,AppModule> modules;

    public App(String id) {
        super(id);
    }
    /** does nothing. */
    public void before(Request request,Response response) throws IOException{
    }
    /** does nothing. */
    public void after(Request request,Response response) throws IOException{
    }
    /** app serves by processing first-last chain then router.
     * always conditional on status being null otherwise it skips.
     */
    public void serve(Request req,Response resp) throws IOException{
        if(first!=null && resp.getStatus()==null) first.process(req, resp);
        if(router!=null && resp.getStatus()==null) router.process(req,resp);
    }
    /** When an error occurs we need properly render exception.
     * if html is accepted we try to render a valid response with n error within a template so it fits with the app.
     * for all others we set error status code.
     * for json,xml and plain we render into a message template for the rest we do nothing.
     * this method returns true if a response was generated. in overloaded methods
     * if false is returned we can generate response the status is set to 500 already.
     * @param req incoming request
     * @param ex exception state
     * @param resp response to generate
     * @return true if handled else it signifies we should do somthing in overloads.
     * @throws IOException
     */
    public boolean processError(com.reliancy.jabba.Request req,Throwable ex,com.reliancy.jabba.Response resp) throws IOException{
        log().error("error:",ex);
        String accepted_format=req.getHeader("Accept");
        boolean present=accepted_format!=null;
        if(present && (
            accepted_format.contains("/html")
            || accepted_format.contains("/xhtml")
            )){
            // we have html request
            resp.setContentType(HTTP.MIME_HTML);
            Template t=Template.find("/templates/error.hbs");
            if(t==null){ // no template found
                resp.setStatus(Response.HTTP_INTERNAL_ERROR);
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new RuntimeException(ex);
            }
            Rendering.begin(t).with(ex).end(resp);
            return true;
        }else{
            // for all other cases we first flag it as error
            resp.setStatus(Response.HTTP_INTERNAL_ERROR);
        } 
        // next we format a few common and supported messages
        if(present && accepted_format.contains("/json")){
            ResponseEncoder enc=resp.getEncoder();
            if(enc.getErrorFormat()==null){
                String template="'{'\n\t\"status\":\"error\",\n\t\"title\":\"{0}\",\n\t\"message\":\"{1}\"\n'}'\n";
                enc.setErrorFormat(template);
            }
            enc.writeError(ex);
            return true;
        }
        if(present && accepted_format.contains("/xml")){
            ResponseEncoder enc=resp.getEncoder();
            if(enc.getErrorFormat()==null){
                String template="<response>\n\t<status>error</status>\n\t<title>{0}</title>\n\t<message>{1}</message>\n</response>\n";
                enc.setErrorFormat(template);
            }
            enc.writeError(ex);
            return true;
        }
        if(present && accepted_format.contains("text/plain")){
            ResponseEncoder enc=resp.getEncoder();
            if(enc.getErrorFormat()==null){
                String template="status=error\n\ntitle={0}\n\nmessage={1}\n\n";
                enc.setErrorFormat(template);
            }
            enc.writeError(ex);
            return true;
        }
        return false;
    }

    /** add one or a chain of processors. */
    public <T extends Processor> T addMiddleWare(T m){
        if(m==null) return null;
        if(first==null){
            last=first=m;
            m.setParent(m);
        }else{
            last.next=m;
        }
        while(last.next!=null){
            last=last.next;
            last.setParent(m);
        }
        return m;
    }
    public void removeMiddleWare(Processor m){
        if(m==null) return;
        if(first==m){
            if(first==last){
                first=last=null;
            }else{
                first=first.next;
            }
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
        m.setParent(null);
    }
    public Processor getProcessor(String id){
        for(Processor c=first;c!=null;c=c.next){
            if(c.getId().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
    /** return special processor which dispatches request.s */
    public Router getRouter() {
        return router;
    }
    /** sets the main request dispatcher. */
    public void setRouter(Router router) {
        if(this.router==router) return;
        if(this.router!=null) this.router.setParent(null);
        this.router = router;
        router.setParent(this);
    }
    public String getWorkPath(String rel_path){
        Config cnf=getConfig();
        String work_dir=Config.APP_WORKDIR.get(cnf);
        if(!work_dir.endsWith("/")) work_dir+="/";
        if(rel_path==null) rel_path="";
        if(rel_path.startsWith("/")) rel_path=rel_path.substring(1);
        if(rel_path==".") rel_path="";
        rel_path=rel_path.replace("\\","/");
        rel_path=rel_path.replace("/./","/");
        String ret=work_dir+rel_path;
        return ret;
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
            p.begin();
        }
        if(router!=null) router.begin(config);
    }
    @Override
    public void end() throws Exception{
        try{
            if(router!=null) router.end();
            for(Processor p=first;p!=null;p=p.getNext()){
                p.end();
            }
            log().info("stopped:"+getId());
            super.end(); // detaches from config
        }finally{
            // we notify all of end (especially cleaner thread)
            synchronized(this){
                this.notifyAll();
            }
        }
    }
    public AppSessionFilter addAppSession(){
        return addMiddleWare(new AppSessionFilter(this));
    }
    public AppSessionFilter addAppSession(AppSession.Factory f){
        return addMiddleWare(new AppSessionFilter(this,f));
    }
    /** set security policy which will recover users and also enforce permissions. */
    public SecurityPolicy setSecurityPolicy(SecurityPolicy secpol){
        if(secpol==policy) return secpol;
        if(policy!=null){
            MethodDecorator.retract(policy);
            removeMiddleWare(policy);
        }
        policy=secpol;
        if(policy!=null){
            addMiddleWare(policy);
            MethodDecorator.publish(policy); // register security policy as decorator factory
        }
        return secpol;
    }
    public SecurityPolicy getSecurityPolicy(){
        return policy;
    }
    public void setStorage(Terminal db){
        storage=db;
    }
    public Terminal getStorage(){
        return storage;
    }
    /** register module under one or more names.
     * if no names are provided just calls module to publish itself.
     * @param module
     * @param names
     */
    public void publishModule(AppModule module,String...names){
        if(names.length==0){
            module.publish(this);
        }else{
            if(modules==null) modules=new HashMap<>();
            for(String nm:names) if(nm!=null) modules.put(nm.toLowerCase(),module);
        }
    }
    /** retracts module under given names.
     * if no names provided calls on module to retract itself.
     */
    public void retractModule(AppModule module,String...names){
        if(names.length==0){
            module.retract(this);
            // if injected from outside we remove those too
            if(modules!=null) while(modules.values().remove(module));
        }else{
            if(modules==null) return;
            for(String nm:names){
                if("*".equals(nm)){ // special case to remove all
                    while(modules.values().remove(module));
                    break;
                }
                modules.remove(nm.toLowerCase());
            }
        }
    }

}
