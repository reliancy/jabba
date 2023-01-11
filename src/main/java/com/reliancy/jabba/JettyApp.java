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
import java.util.HashMap;
import java.util.Map;

import com.reliancy.jabba.sec.NotAuthentic;
import com.reliancy.jabba.sec.Secured;
import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.plain.PlainSecurityStore;
import com.reliancy.jabba.ui.Feedback;
import com.reliancy.jabba.ui.FeedbackLine;
import com.reliancy.jabba.ui.Menu;
import com.reliancy.jabba.ui.MenuItem;
import com.reliancy.jabba.ui.Rendering;
import com.reliancy.jabba.ui.Template;

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
    @Override
    public Server getServer() {
        return jetty;
    }

    @Override
    public void setServer(Server arg0) {
        jetty=arg0;
    }
    @Override
    public boolean addEventListener(EventListener arg0) {
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
    }

    @Override
    public void stop() throws Exception {
        _state=State.STOPPED;
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
        baseRequest.setHandled(true);
        com.reliancy.jabba.Request req=new com.reliancy.jabba.Request(request);
        Response resp=new Response(response);

        CallSession ss=CallSession.getInstance();
        try{
            ss.begin(null, req, resp);
            process(req,resp);
        }catch(IOException ioex){ 
            Template t=Template.find("/templates/error.hbs");
            if(t==null) throw ioex;
            Rendering.begin(t)
                .with(ioex)
                .end(resp.getEncoder().getWriter());
            log().error("error:",ioex);
        }catch(RuntimeException rex){
            Template t=Template.find("/templates/error.hbs");
            if(t==null) throw rex;
            Rendering.begin(t)
                .with(rex)
                .end(resp.getEncoder().getWriter());
            log().error("error:",rex);
        }finally{
            ss.end();
        }
    }
    /** our own interface specific to jetty engine*/

    public Connector[] getConnectors(){
        if(connectors!=null) return connectors;
        ServerConnector connector = new ServerConnector(jetty);
        connector.setReuseAddress(false);
        connector.setPort(8090);
        connectors=new Connector[] {connector};
        return connectors;
    }
    public void begin(Config conf) throws Exception{
        super.begin(conf);
        jetty.setConnectors(getConnectors());
        try{
            jetty.start();
        }catch(Exception ex){
            setState(State.FAILED);
            if(ex.getCause() instanceof java.net.BindException){
                log().error("Bind issue",ex);
                Thread.sleep(3000);
            }else throw ex;
        }
    }
    public void work() throws InterruptedException{
        setState(State.RUNNING);
        if(jetty!=null) jetty.join();
    }
    public void end() throws Exception{
        //setState(State.STOPPING);
        super.end();
        Connector[] connectors=jetty.getConnectors();
       // System.out.println(connectors);
        if(connectors!=null) for(Connector c:connectors){
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
        //System.out.println("signaling...");
        jetty.stop();
        //setState(State.STOPPED);
        //System.out.println("cleanup...");
        System.gc();
        //System.out.println("return...");
    }
    public static void main( String[] args ) throws Exception{
        //System.out.println("Hello World!");
        //String rt=new File(".").getAbsolutePath();
        //System.out.println("ROOT:"+rt);
        String work_dir="./var";
        if(new File(work_dir).exists()==false){
            work_dir="../var";
        }
        Template.search_path(work_dir,App.class);
        JettyApp app=new JettyApp();
        app.addAppSession();
        SecurityPolicy secpol=new SecurityPolicy().setStore(new PlainSecurityStore());
        app.setSecurityPolicy(secpol);
        RoutedEndPoint rep=new RoutedEndPoint().importMethods(app);
        app.setRouter(rep);
        FileServer fs=new FileServer("/static",work_dir+"/public");
        fs.exportRoutes(app.getRouter());
        Menu top_menu=Menu.request(Menu.TOP);
        top_menu.add(new MenuItem("home")).addSpacer().add(new MenuItem("login"));
        top_menu.setTitle("Jabba");
        app.run(new FileConfig());
        //System.out.println("Goodbye World!");
    }

    @Routed()
    public String hello(){
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Jared");
        String ret="";
        try {
                Template t=Template.find("/templates/login.hbs");
                System.out.println("Template:"+t);
                ret = t.render(context).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
        //#return "Hello World";
    }
    @Routed(
        path="/helloPlain"
    )
    public void hello2(com.reliancy.jabba.Request req,Response resp) throws IOException{
        resp.getEncoder().writeln("Hi There");
    }
    @Routed(
        path="/hello3/{idd:int}"
    )
    public String hello3(int id){
        return "Hello3:"+id;
    }
    @Routed(
        path="/"
    )
    public String home(){
        StringBuilder buf=new StringBuilder();
        buf.append("<p>Sample pages:</p>");
        buf.append("<dd><a href='/helloPlain'>plain</a></dd>");
        buf.append("<dd><a href='/hello3/5'>parametric</a></dd>");
        buf.append("<dd><a href='/hello'>templated</a></dd>");
        buf.append("<dd><a href='/secured'>secured http</a></dd>");
        buf.append("<dd><a href='/secured_form'>secured form</a></dd>");
        return buf.toString();
    }
    @Routed
    @Secured
    public String secured(){
        return "We are secured";
    }
    @Routed
    @Secured(
        login_form = "/login"
    )
    public String secured_form(){
        return "We are secured by form";
    }
    @Routed
    public void login(com.reliancy.jabba.Request req,Response resp){
        //return "login form here";
        if(req.getVerb().equals("POST")){
            // here we need to process login and redirect
            try{
            System.out.println("Post login");
            String userid=(String)req.getParam("userid",null);
            String pwd=(String)req.getParam("password",null);
            AppSession ass=AppSession.getInstance();
            System.out.println("SS:"+ass);
            System.out.println("P:"+userid+"/"+pwd);
            SecurityPolicy secpol=ass.getApp().getSecurityPolicy();
            SecurityActor user=secpol.authenticate(userid, pwd);
            if(user==null) throw new NotAuthentic("invalid credentials");
            resp.setStatus(Response.HTTP_FOUND_REDIRECT);
            //String old_url=request.getPath();
            //old_url=URLEncoder.encode(old_url,StandardCharsets.UTF_8.toString());
            resp.setHeader("Location","/home");
            }catch(Exception ex){
                log().error("error:",ex);
                Feedback.get().push(FeedbackLine.error(ex.getLocalizedMessage()));        
            }
        }
        //Map<String, Object> context = new HashMap<>();
        //context.put("app_title", "Jabba Login");
        //context.put("name", "Jared");
        //ArrayList<FeedbackLine> events=new ArrayList<>();

        //Feedback.get().push(FeedbackLine.error("Error"));
        //Feedback.get().push(FeedbackLine.info("Error"));
        //Feedback.get().push(FeedbackLine.warn("Error"));
        //context.put("feedback",events);
        try {
            resp.setContentType("text/html");
            Rendering.begin("/templates/login.hbs")
                //.with("feedback",events)
                .end(resp.getEncoder().getWriter());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
