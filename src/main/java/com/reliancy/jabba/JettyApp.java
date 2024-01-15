/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.File;
import java.io.IOException;
import java.util.EventListener;

import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.plain.PlainSecurityStore;
import com.reliancy.jabba.ui.Menu;
import com.reliancy.jabba.ui.MenuItem;
import com.reliancy.jabba.ui.Rendering;
import com.reliancy.jabba.ui.Template;
import com.reliancy.util.Log;
import com.reliancy.util.Resources;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Router is entry point and servlet implementation that dispatches messages to our endpoints.
 * It will launch an embedded jetty server.
 * It will provide facilities to register endpoints.
 */
public class JettyApp extends App implements Handler{
    enum State{
        STOPPED,
        FAILED,
        STARTING,
        STARTED,
        STOPPING,
        RUNNING
    }
    protected Connector[] connectors;
    protected Server jetty;
    private volatile State _state;

    public JettyApp() {
        super("JettyApp");
        jetty = new Server();
        jetty.setHandler(this);
        _state=State.STOPPED;
    }
    /** implementation of jetty handler interface */
    public Connector[] getConnectors(){
        if(connectors!=null) return connectors;
        ServerConnector connector = new ServerConnector(jetty);
        connector.setReuseAddress(false);
        connector.setPort(8090);
        connectors=new Connector[] {connector};
        return connectors;
    }
    @Override
    public Server getServer() {
        return jetty;
    }

    @Override
    public void setServer(Server arg0) {
        System.out.println("setServer..."+jetty+"/"+arg0);
        jetty=arg0;
    }
    @Override
    public boolean addEventListener(EventListener arg0) {
        System.out.println("adding evt listener...");
        return false;
    }
    @Override
    public boolean removeEventListener(EventListener arg0) {
        return false;
    }
    protected void setState(State s){
        _state=s;
    }
    @Override
    public boolean isFailed() {
        return _state==State.FAILED;
    }

    @Override
    public boolean isRunning() {
        return _state==State.RUNNING;
    }

    @Override
    public boolean isStarted() {
        return _state==State.STARTED;
    }

    @Override
    public boolean isStarting() {
        return _state==State.STARTING;
    }

    @Override
    public boolean isStopped() {
        return _state==State.STOPPED;
    }

    @Override
    public boolean isStopping() {
        return _state==State.STOPPING;
    }
    @Override
    public void start() throws Exception {
        _state=State.STARTED;
        jetty.setConnectors(getConnectors());
    }

    @Override
    public void stop() throws Exception {
        _state=State.STOPPED;
        Connector[] connectors=jetty.getConnectors();
        if(connectors==null || connectors.length==0) return;
        for(Connector c:connectors){
            ServerConnector cc=(ServerConnector) c;
            //System.out.println("stopping connecor:"+cc);
            try{
                cc.stop();
                cc.getConnectedEndPoints().forEach((endpoint)-> {
                    //System.out.println("closing endpoint:"+endpoint);
                    endpoint.close();
                });
            }finally{
                cc.close();
                //System.out.println("closing connecor:"+cc.getState());
            }
        }
    }

    @Override
    public void destroy() {
    }
    /**
     * Our implementation of a handle process.
     * In case of exception if we can locate /tempaltes/error.hbs we use it else we re-throw.
     */
    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException
    {
        com.reliancy.jabba.Request req=new com.reliancy.jabba.Request(request);
        Response resp=new Response(response);

        CallSession ss=CallSession.getInstance();
        try{
            ss.begin(null, req, resp);
            process(req,resp);
        }catch(IOException ioex){ 
            Template t=Template.find("/templates/error.hbs");
            if(t==null) throw ioex;
            Rendering.begin(t).with(ioex).end(resp);
            log().error("error:",ioex);
        }catch(RuntimeException rex){
            Template t=Template.find("/templates/error.hbs");
            if(t==null) throw rex;
            Rendering.begin(t).with(rex).end(resp);
            log().error("error:",rex);
        }finally{
            baseRequest.setHandled(true);
            ss.end();
        }
    }
    /** our own interface specific to jetty engine*/
    public void begin(Config conf) throws Exception{
        // step 2: configure application, might add processors, adjust config
        configure(conf);
        // step 1: install config then begin by signaling all middleware
        super.begin(conf);
        // step 2: start jetty
        try{
            log().info("starting...");
            jetty.start();
        }catch(Exception ex){
            setState(State.FAILED);
            if(ex.getCause() instanceof java.net.BindException){
                log().error("bind issue",ex);
                Thread.sleep(3000);
            }else throw ex;
        }
    }
    public void work() throws InterruptedException{
        setState(State.RUNNING);
        if(jetty!=null) jetty.join();
    }
    public void end() throws Exception{
        super.end();
        Log.cleanup();  // release logging in case we deferred
        System.gc();    // sweep memory just in caser
    }
    /** called from begin just before jetty starts. 
     * this method is called before middleware is notified so we can add or adjust config.
     * override to hook up your application.
     * normally follows configuraion and does common sense steps.
     * might install middleware (processors) which are later passed config.
    */
    public void configure(Config conf) throws Exception{
        App app=this;
        // setup global search path - include workdir first, then get class and app.class
        Class<?> cls=getClass();
        if(cls!=JettyApp.class) Resources.appendSearch(0,JettyApp.class);
        Resources.appendSearch(0,cls);
        String work_dir=ArgsConfig.APP_WORKDIR.get(conf);
        if(work_dir!=null) Resources.appendSearch(0,work_dir);
        //for(Object p:Resources.search_path){
        //    System.out.println("sp:"+p);
        //}
        //Template.search_path(work_dir,App.class);   -- not needed anymore
        // install app session middleware
        app.addAppSession();
        // set security policy
        SecurityPolicy secpol=new SecurityPolicy().setStore(new PlainSecurityStore());
        app.setSecurityPolicy(secpol);
        // install router
        app.setRouter(new Router());
        DemoEP ep=new DemoEP();
        ep.publish(app);
        // install file sever endpoint
        FileServer fs=new FileServer("/static","/public");
        fs.publish(app);
        Menu top_menu=Menu.request(Menu.TOP);
        top_menu.add(new MenuItem("home")).addSpacer().add(new MenuItem("login"));
        top_menu.setTitle("Jabba3");
    }
    public static void main( String[] args ) throws Exception{
        Config cnf=new ArgsConfig(args).load();
        JettyApp app=new JettyApp();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(app.isRunning()){
                try {
                    app.jetty.stop();
                    synchronized(app){
                        app.wait(5000);
                    }
                } catch (Exception e) {
                    app.log().error("shutdown cleanup:", e);
                }
            }
        }));
        app.run(cnf);
    }


}
