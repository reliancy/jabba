package com.reliancy.jabba;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.reliancy.jabbasec.SecurityPolicy;
import com.reliancy.util.Template;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Router is entry point and servlet implementation that dispatches messages to our endpoints.
 * It will launch an embedded jetty server.
 * It will provide facilities to register endpoints.
 */
public class Router extends AbstractHandler{
    protected Connector[] connectors;
    protected Server jetty;
    protected Processor first=null;
    protected Processor last=null;
    protected RouterEndPoint main=null;
    protected transient Config config=null;
    protected Logger logger=LoggerFactory.getLogger(Router.class);

    public Router() {
        jetty = new Server();
        jetty.setHandler(this);
    }
    public Config getConfig(){
        return config;
    }
    public void addProcessor(Processor m){
        if(first==null){
            last=first=m;
        }else{
            last.next=m;
        }
        while(last.next!=null) last=last.next;
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
    
    public RouterEndPoint getMain() {
        return main;
    }
    public void setMain(RouterEndPoint resolver) {
        this.main = resolver;
    }
    public RouterEndPoint importEndPoints(Object target){
        RouterEndPoint ret=new RouterEndPoint();
        LinkedList<Method> routes=new LinkedList<>();
        Class<?> type=target.getClass();
        while (type != null) {
            for(Method m : type.getDeclaredMethods()){
                //System.out.println("Method:"+m.toString());
                if(m.getAnnotation(Route.class)!=null){
                    routes.add(0,m);
                }
            }
            type = type.getSuperclass();
        }
        for(Method m:routes){
            //System.out.println("M:"+m);
            Route r=m.getAnnotation(Route.class);
            MethodEndPoint mm=new MethodEndPoint(target,m,r);
            ret.addRoute(r.verb(),mm.getPath(),mm);
        }
        return ret;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException
    {
        baseRequest.setHandled(true);
        com.reliancy.jabba.Request req=new com.reliancy.jabba.Request(request);
        Response resp=new Response(response);
        CallSession ss=CallSession.getInstance();
        try{
            ss.begin(null, req, resp);
            if(first!=null) first.process(req, resp);
            if(main!=null) main.process(req,resp);
        }finally{
            ss.end();
        }
    }

    public Connector[] getConnectors(){
        if(connectors!=null) return connectors;
        ServerConnector connector = new ServerConnector(jetty);
        connector.setReuseAddress(false);
        connector.setPort(8090);
        connectors=new Connector[] {connector};
        return connectors;
    }
    public void begin(Config conf) throws Exception{
        if(config!=null) throw new RuntimeException("Router running already");
        config=conf;
        for(Processor p=first;p!=null;p=p.getNext()){
            p.begin(config);
        }
        if(main!=null) main.begin(config);
        jetty.setConnectors(getConnectors());
        try{
            jetty.start();
        }catch(Exception ex){
            if(ex.getCause() instanceof java.net.BindException){
                logger.error("Bind issue",ex);
                Thread.sleep(3000);
            }
        }
    }
    public void end() throws Exception{
        if(main!=null) main.end();
        for(Processor p=first;p!=null;p=p.getNext()){
            p.end();
        }
        config=null;
        logger.info("stopiing jetty");
        Connector[] connectors=jetty.getConnectors();
        System.out.println(connectors);
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
        //System.out.println("cleanup...");
        System.gc();
        //System.out.println("return...");
    }
    public void run(Config conf) throws Exception {
        try{
            begin(conf);
            //System.out.println("Entering server loop...");
            jetty.join();
        }finally{
            //System.out.println("Exiting server loop...");
            end();
            //System.out.println("Exiting server loop...done");
        }
    }
    public static void main( String[] args ) throws Exception
    {
        //System.out.println("Hello World!");
        Router app=new Router();
        app.addProcessor(new AppSessionFilter());
        app.addProcessor(new SecurityPolicy());
        app.setMain(app.importEndPoints(app));
        FileServer fs=new FileServer("/static","./var");
        fs.exportRoutes(app.getMain());
        app.run(null);
        //System.out.println("Goodbye World!");
    }

    @Route()
    public String hello(){
        Map<String, Object> context = new HashMap<>();
        context.put("name", "Jared");
        String ret="";
        try {
                Template.search_path("./var",SecurityPolicy.class);
                Template t=Template.find("/templates/login.hbs");
                System.out.println("Template:"+t);
                ret = t.render(context).toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
        //#return "Hello World";
    }
    @Route(
        path="/helloPlain"
    )
    public void hello2(com.reliancy.jabba.Request req,Response resp) throws IOException{
        resp.getEncoder().writeln("Hi There");
    }
    @Route(
        path="/hello3/{idd:int}"
    )
    public String hello3(int id){
        return "Hello3:"+id;
    }
}
