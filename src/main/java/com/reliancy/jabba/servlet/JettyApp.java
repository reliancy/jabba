/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.servlet;

import java.io.IOException;

import com.reliancy.jabba.App;
import com.reliancy.jabba.ArgsConfig;
import com.reliancy.jabba.CallSession;
import com.reliancy.jabba.Config;
import com.reliancy.jabba.FileServer;
import com.reliancy.jabba.Response;
import com.reliancy.jabba.Router;
import com.reliancy.jabba.StatusMod;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.plain.PlainSecurityStore;
import com.reliancy.jabba.ui.Menu;
import com.reliancy.jabba.ui.MenuItem;
import com.reliancy.util.Log;
import com.reliancy.util.Resources;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Router is entry point and servlet implementation that dispatches messages to our endpoints.
 * It will launch an embedded jetty server.
 * It will provide facilities to register endpoints via router.
 * Mostly new routes are injected via AppModules which publish themselves.
 * JettyApp installs ForwardCustomizer to react to reverse proxy setups.
 * 
 */
public class JettyApp extends App implements Servlet {
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
    protected ServletConfig servletConfig;
    private volatile State _state;

    public JettyApp() {
        super("JettyApp");
        jetty = new Server();
        _state=State.STOPPED;
        this.addShutdownHook();
    }
    public Connector[] getConnectors(){
        if(connectors!=null) return connectors;
        // Create HTTP Config
        HttpConfiguration httpConfig = new HttpConfiguration();
        // Add support for X-Forwarded headers
        httpConfig.addCustomizer( new ForwardedRequestCustomizer() );
        // Create the http connector
        HttpConnectionFactory http11 = new HttpConnectionFactory( httpConfig );
        HTTP2ServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);
        ServerConnector httpConn = new ServerConnector(jetty,http11,h2c);
        httpConn.setReuseAddress(false);
        // Get port from config, environment variable, or default to 8090
        int port=8090;
        Config conf=getConfig();
        if(conf!=null){
            port=Config.SERVER_PORT.get(conf,8090);
        }
        // Check environment variable
        String envPort=System.getenv("JABBA_SERVER_PORT");
        if(envPort!=null && !envPort.isEmpty()){
            try{
                port=Integer.parseInt(envPort);
            }catch(NumberFormatException e){
                log().warn("Invalid JABBA_SERVER_PORT environment variable: {}, using default",envPort);
            }
        }
        // Check system property
        String sysPort=System.getProperty("jabba.server.port");
        if(sysPort!=null && !sysPort.isEmpty()){
            try{
                port=Integer.parseInt(sysPort);
            }catch(NumberFormatException e){
                log().warn("Invalid jabba.server.port system property: {}, using default",sysPort);
            }
        }
        httpConn.setPort(port);
        log().info("Server configured to listen on port {}",port);
        connectors=new Connector[] {httpConn};
        return connectors;
    }
    /** implementation of jetty handler interface */
    public Server getServer() {
        return jetty;
    }

    public void setServer(Server arg0) {
        jetty=arg0;
    }
    protected void setState(State s){
        _state=s;
    }
    public boolean isFailed() {
        return _state==State.FAILED;
    }

    public boolean isRunning() {
        return _state==State.RUNNING;
    }

    public boolean isStarted() {
        return _state==State.STARTED;
    }

    public boolean isStarting() {
        return _state==State.STARTING;
    }

    public boolean isStopped() {
        return _state==State.STOPPED;
    }

    public boolean isStopping() {
        return _state==State.STOPPING;
    }
    public void start() throws Exception {
        _state=State.STARTING;
        jetty.setConnectors(getConnectors());
        jetty.start();
        _state=State.STARTED;
    }

    public void stop() throws Exception {
        log().info("Stopping Jetty server...");
        _state=State.STOPPING;
        Connector[] connectors=jetty.getConnectors();
        if(connectors==null || connectors.length==0){
            _state=State.STOPPED;
            log().info("No connectors to stop.");
            return;
        }
        for(Connector c:connectors){
            ServerConnector cc=(ServerConnector) c;
            try{
                int port = ((ServerConnector)c).getPort();
                log().info("Closing connector on port {}...", port);
                cc.stop();
                cc.getConnectedEndPoints().forEach((endpoint)-> {
                    endpoint.close();
                });
            }finally{
                cc.close();
            }
        }
        _state=State.STOPPED;
        log().info("Jetty server stopped.");
    }

    public void destroy() {
    }
    
    // Servlet interface methods
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.servletConfig = config;
    }
    
    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }
    
    @Override
    public String getServletInfo() {
        return "JettyApp - Jabba Framework Servlet";
    }
    
    /**
     * Our servlet service implementation.
     * In case of exception if we can locate /templates/error.hbs we use it else we re-throw.
     */
    @Override
    public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        // Cast to HTTP versions (this servlet only handles HTTP)
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        final com.reliancy.jabba.servlet.ServletRequest req = 
            new com.reliancy.jabba.servlet.ServletRequest(httpRequest);
        final com.reliancy.jabba.servlet.ServletResponse resp = 
            new com.reliancy.jabba.servlet.ServletResponse(req, httpResponse);
        final CallSession ss=CallSession.getInstance();
        // install executor just in case we need it, especially for async processing
        ss.setExecutor(
            jetty.getThreadPool() != null ? 
            jetty.getThreadPool() : java.util.concurrent.ForkJoinPool.commonPool()
            );
        req.setSession(ss);
        try{
            ss.beginFresh(null, req, resp);
            process(req,resp);
        }catch(Exception ioex){ 
            try{
                resp.getEncoder().writeError(ioex);
            }catch(IOException e){
                resp.setStatus(Response.HTTP_INTERNAL_ERROR);
            }
        }finally{
            // Only mark as handled if not async (async will be completed later)
            // Only end session if not async (async will end session when completing)
            if(resp.isPromised()==false){
                ss.end();
                resp.complete();
            }else{
                resp.promiseLast((result, error) -> {
                    if(result instanceof Exception){
                        error=(Exception)result;
                        result=null;
                    }
                    if(error!=null){
                        try{
                            resp.getEncoder().writeError(error);
                        }catch(IOException e){
                            resp.setStatus(Response.HTTP_INTERNAL_ERROR);
                        }
                    }else if(result!=null){
                        // it should never get here we expect null unless we have an error
                        try{
                            resp.getEncoder().writeObject(result);
                        }catch(IOException e){
                            resp.setStatus(Response.HTTP_INTERNAL_ERROR);
                        }
                    }
                    resp.complete();
                });
            }
        }
    }
    /** our own interface specific to jetty engine*/
    public void begin(Config conf) throws Exception{
        // step 1: configure application, might add processors, adjust config
        configure(conf);
        // step 2: install config then begin by signaling all middleware
        super.begin(conf);
        // step 3: create servlet context and mount this servlet
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(this), "/*");
        // step 3a: initialize Jakarta WebSocket support
        // IMPORTANT: must be called before context is started
        JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, serverContainer) -> {
            // Optional: tune WebSocket defaults
            // serverContainer.setDefaultMaxSessionIdleTimeout(Duration.ofMinutes(5));
            // serverContainer.setDefaultMaxTextMessageBufferSize(64 * 1024);
            log().info("WebSocket support initialized");
        });
        jetty.setHandler(context);
        // step 4: set connectors and start jetty
        try{
            log().info("starting...");
            start();
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
        log().info("Server is running. Press Ctrl-C to exit.");
        if(jetty!=null) jetty.join();
    }
    public void end() throws Exception{
        log().info("JettyApp cleanup starting...");
        stop();
        log().info("Cleaning up application processors...");
        super.end();
        log().info("Application cleanup complete.");
        Log.cleanup();
        System.gc();
    }
    /** Registers a shutdown hook to interrupt jetty.
     * ctrl-c works but does not perform our shutdown sequence.
     * this code interrupts jetty and then waits for app to finish.
     */
    protected final void addShutdownHook(){
        final JettyApp app=this;
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
        // install app session middleware
        app.addAppSession();
        // set security policy
        SecurityPolicy secpol=new SecurityPolicy().setStore(new PlainSecurityStore());
        app.setSecurityPolicy(secpol);
        // install router
        app.setRouter(new Router());
        StatusMod ep=new StatusMod();
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
        app.run(cnf);
    }


}


