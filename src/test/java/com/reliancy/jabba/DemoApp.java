package com.reliancy.jabba;

import java.io.IOException;
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
import com.reliancy.util.Resources;

/** Demo application.
 * We test out main features of jabba.
 */
public class DemoApp extends JettyApp implements AppModule{
    public static void main( String[] args ) throws Exception{
        Config cnf=new ArgsConfig(args).load();
        JettyApp app=new DemoApp();
        app.addShutdownHook();
        app.run(cnf);
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
        log().info("work_dir:{}",work_dir);
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
        StatusMod ep=new StatusMod();
        ep.publish(app);
        // install file sever endpoint
        FileServer fs=new FileServer("/static","/public");
        fs.publish(app);
        Menu top_menu=Menu.request(Menu.TOP);
        top_menu.add(new MenuItem("home")).addSpacer().add(new MenuItem("login"));
        top_menu.setTitle("Jabba3");
        app.getRouter().compile();
        System.out.println(app.getRouter().regex);
    }
    @Override
    public void publish(App app) {
        app.getRouter().importMethods(this);
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
        log().info("login here");
        if(req.getVerb().equals("POST")){
            // here we need to process login and redirect
            AppSession ass=AppSession.getInstance();
            try{
            System.out.println("Post login");
            String userid=(String)req.getParam("userid",null);
            String pwd=(String)req.getParam("password",null);
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
                ass.getApp().log().error("error:",ex);
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
                .end(resp);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
    
}
