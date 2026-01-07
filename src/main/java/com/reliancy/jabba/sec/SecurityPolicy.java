/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.regex.Pattern;

import com.reliancy.jabba.AppSession;
import com.reliancy.jabba.CallSession;
import com.reliancy.jabba.Processor;
import com.reliancy.jabba.Request;
import com.reliancy.jabba.Response;
import com.reliancy.jabba.RouteDetector;
import com.reliancy.jabba.MethodDecorator;
import com.reliancy.jabba.MethodEndPoint;
import com.reliancy.jabba.Config;
import com.reliancy.util.CodeException;
import com.reliancy.util.Handy;
import java.security.SecureRandom;

/**
 * SecurityPolicy is a filter/processor that implements various auth protocols but also sources users.
 * The policy will produce new users once they are authenticated.
 * SecurityPolicy will authenticate SecurityActors and possibly authorize them via a permission mechanism
 * to access.
 * SecurityProtocol is one authenticatio method. We will not adjust response only try to recover user.
 * Initialization of auth will occur outside:
 *  - for gui after login we will set an auth cookie to remember user and password
 *  - for APIs if user is required will issue error 401 with WWW-Authenticate header
 */
public class SecurityPolicy extends Processor implements MethodDecorator.Factory{
    public static String REALM="reliancy";
    public static final String KEY_NAME="jbauth";
    protected String secret=null;
    protected ArrayList<SecurityProtocol> protocols;
    protected SecurityActor admin;
    protected SecurityActor guest;
    protected final HashMap<Pattern,Secured> secured_pat=new HashMap<>(); // paths that require user
    protected SecurityStore store;

    public SecurityPolicy() {
        super(SecurityPolicy.class.getSimpleName().toLowerCase());
        protocols=new ArrayList<>();
        protocols.add(new SecurityProtocol.Digest());
        protocols.add(new SecurityProtocol.Basic());
    }
    protected String getSecret(){
        if(secret==null){
            // Try to load from config first
            Config conf=getConfig();
            if(conf!=null){
                secret=Config.SECRET_KEY.get(conf,null);
            }
            // Try environment variable
            if(secret==null || secret.isEmpty()){
                secret=System.getenv("JABBA_SECRET_KEY");
            }
            // Try system property
            if(secret==null || secret.isEmpty()){
                secret=System.getProperty("jabba.secret.key");
            }
            // Generate secure random secret if still not found
            if(secret==null || secret.isEmpty()){
                SecureRandom random=new SecureRandom();
                byte[] bytes=new byte[32];
                random.nextBytes(bytes);
                secret=java.util.Base64.getEncoder().encodeToString(bytes);
                log().warn("No secret key configured. Generated a random secret. This should be set via SECRET_KEY config, JABBA_SECRET_KEY environment variable, or jabba.secret.key system property for production use.");
            }
        }
        return secret;
    }
    public SecurityPolicy setSecured(String path,Secured info){
        if(checkSecured(path)!=null) throw new IllegalStateException("Secured path cannot be secured again:"+path);
        Pattern regex=Pattern.compile(path);
        secured_pat.put(regex,info);
        return this;
    }
    public Secured checkSecured(String path){
        for(Pattern p:secured_pat.keySet()){
            if(p.pattern().equals(path)) return secured_pat.get(p);
            if(p.matcher(path).find()) return secured_pat.get(p);
        }
        return null;
    }
    @Override
    public void beforeServe(Request request, Response response) throws IOException {
        // we will recover a user here
        CallSession css=CallSession.getInstance();
        AppSession ass=(AppSession) css.getAppSession();
        if(ass==null || ass.getUser()!=null){
            return; // we got a user all good
        }
        try{
            SecurityActor user=authenticate(request);
            if(user!=null) ass.setUser(user);
        }catch(NotAuthentic bad_cred){
            // we could not establish user
            response.setStatus(Response.HTTP_FORBIDDEN);
            response.getEncoder().writeObject(CodeException.getUserMessage(bad_cred));
        }catch(NeedCredentials no_cred){
            String login_form=no_cred.get("login_form");
            if(Handy.isBlank(login_form)){
                // we got no login form use HTTP auth
                response.setStatus(Response.HTTP_UNAUTHORIZED);
                String auth_supported=protocols.get(0).getSignature(REALM);
                //String auth_supported=protocols.stream().map(SecurityProtocol::getName).collect(Collectors.joining(","));
                response.setHeader("WWW-Authenticate",auth_supported);
            }else{
                // we got a login form do a redirect
                response.setStatus(Response.HTTP_FOUND_REDIRECT);
                String old_url=request.getPath();
                old_url=URLEncoder.encode(old_url,StandardCharsets.UTF_8.toString());
                //old_url=flask.escape(request.url.replace(request.url_root.strip("/"),""))
                //resp=flask.redirect("{}?next={}".format(login_pg,old_url),code=303)
                response.setHeader("Location",login_form+"?next="+old_url);
            }
        }
    }
    @Override
    public void afterServe(Request request, Response response) throws IOException {
    }
    /** authenticates or establishes user based on user and password.
     * same as loadActor but with first param being admin account.
     * @param name userid
     * @param pwd password
     * @return user we could establish
     * @throws NotPermitted
     * @throws IOException
     */
    public SecurityActor authenticate(String name, String pwd) throws NotPermitted, IOException{
        return loadActor(admin, name, pwd);
    }
        /** authenticates or establishes user based on request and updates response.
     * this method might redirect to a login view. once it is done
     * @param req
     * @return user we could establish
     * @throws NotPermitted
     * @throws IOException
     */
    public SecurityActor authenticate(Request req) throws IOException, NotAuthentic, NeedCredentials{
        // must recover user from cookies or by redirecting to login
        String verb=req.getVerb();
        String path=req.getPath();
        log().info("Path:"+path);
        String secpath=verb+" "+path;
        Secured secinfo=checkSecured(secpath);
        if(secinfo==null){
            return null; // this path is not secured
        }
        log().info("\tuser is needed, send back auth resp");
        String auth=req.getCookie(KEY_NAME,null);
        //log().info("Auth1:"+auth);
        if(auth!=null){
            // we have an auth cookie - encoded user login
            Map<String,String> kv=Handy.decrypt(getSecret(),auth);
            String username=(kv.get("n"));
            String password=(kv.get("p"));
            String address=(kv.get("a"));
            if(address!=null && !address.equals(req.getRemoteAddress())){
                return null; // invalid auth cookie
            }
            try {
                SecurityActor user = loadActor(admin,username,password);
                if(user!=null) return user;
                else throw new NotAuthentic("invalid credentials");
            } catch (NotPermitted e) {
                throw new NotAuthentic("not permitted to authenticate",e);
            }
        }
        // try authorization header as fallback - can't clear it always
        auth=req.getHeader("Authorization");
        //log().info("Auth2:"+auth);
        if(auth!=null){
            String[] kv=auth.split(" ",2);
            String proto_name=kv[0];
            String proto_args=kv.length>1?kv[1]:"";
            for(SecurityProtocol sproto:protocols){
                if(proto_name.equalsIgnoreCase(sproto.getName())){
                    return sproto.authenticate(this, req,proto_args);
                }
            }
            throw new NotAuthentic("auth method not supported:"+proto_name);
        }
        throw new NeedCredentials().put("login_form",secinfo.login_form());
        //return null;
    }
    /** will establish what if any rights an actor has on a securable. */
    public SecurityPermit authorize(SecurityActor actor, Securable subject){
        return actor.getPermit(subject);
    }
    public SecurityPolicy setStore(SecurityStore store) throws NotPermitted, IOException{
        // this call will work unless store is locked already
        guest=(SecurityActor)store.loadSecurable(null,SecurityStore.GUEST);
        admin=(SecurityActor)store.loadSecurable(null,SecurityStore.ADMIN);
        if(guest==null) throw new IllegalArgumentException("store is missing guest actor");
        if(admin==null) throw new IllegalArgumentException("store is missing admin actor");
        // now we lock store
        store.setPolicy(this);
        this.store=store;
        return this;
    }
    public SecurityStore getStore(){
        return store;
    } 
    /** will save a securable including a user if permitted via actor. */
    public void saveSecurable(SecurityActor actor, Securable sec) throws IOException{
        store.saveSecurable(actor, sec);
    }
    /** loads a securable by id given actor permits. */
    public Securable loadSecurable(SecurityActor actor, Integer id) throws IOException, NotPermitted{
        return store.loadSecurable(actor, id);
    }
    /** loads an actor given name and/or password. 
    * if actor is an admin and no password is given it looks up actor by name. 
    */
    public SecurityActor loadActor(SecurityActor actor, String name, String pwd) throws IOException, NotPermitted{
        return store.loadActor(actor, name, pwd);
    }
    /**
     * we do not create actual decorators we just register this method so we can intercept routes.
     */
    @Override
    public MethodDecorator assertDecorator(MethodEndPoint mep, Annotation ann) {
        if(!(ann instanceof Secured)) return null;
        log().debug("Assert decorator for:{}",mep.getPath());
        String verb=mep.getVerb();
        String path=mep.getPath();
        String pat=RouteDetector.toPattern(verb, path);
        setSecured(pat,(Secured)ann);
        return null;
    }

}
