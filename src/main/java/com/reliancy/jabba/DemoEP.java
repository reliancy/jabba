package com.reliancy.jabba;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.reliancy.jabba.sec.NotAuthentic;
import com.reliancy.jabba.sec.Secured;
import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.ui.Feedback;
import com.reliancy.jabba.ui.FeedbackLine;
import com.reliancy.jabba.ui.Rendering;
import com.reliancy.jabba.ui.Template;

public class DemoEP implements AppModule{
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
