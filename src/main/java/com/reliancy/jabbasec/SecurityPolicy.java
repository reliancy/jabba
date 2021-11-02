package com.reliancy.jabbasec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.reliancy.jabba.AppSession;
import com.reliancy.jabba.CallSession;
import com.reliancy.jabba.Processor;
import com.reliancy.jabba.Request;
import com.reliancy.jabba.Response;
import com.reliancy.util.Handy;

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
public class SecurityPolicy extends Processor{
    public static final String KEY_NAME="jbauth";
    protected String secret="sdfklgj 7150 9178-54=09";
    protected ArrayList<SecurityProtocol> protocols;
    protected SecurityActor admin;
    protected SecurityActor guest;

    public SecurityPolicy() {
        super(SecurityPolicy.class.getSimpleName().toLowerCase());
        protocols=new ArrayList<>();
        protocols.add(new SecurityProtocol.Digest());
        protocols.add(new SecurityProtocol.Basic());
    }
    @Override
    public void before(Request request, Response response) throws IOException {
        // we will recover a user here
        CallSession css=CallSession.getInstance();
        AppSession ass=(AppSession) css.getAppSession();
        if(ass==null || ass.getUser()!=null){
            return; // we got a user all good
        }
        try{
            SecurityActor user=authenticate(request);
            if(user!=null) ass.setUser(user);
        }catch(NotAuthentic ex){
            // we could not establish user
            response.setStatus(Response.HTTP_FORBIDDEN);
            response.getEncoder().writeObject(ex);
        }
    }
    @Override
    public void after(Request request, Response response) throws IOException {
    }
    @Override
    public void serve(Request request, Response response) throws IOException {
        // nothing to do here
    }
    protected String getSecret(){
        return secret;
    }
    /** authenticates or establishes user based on request and updates response.
     * this method might redirect to a login view. once it is done
     * @param req
     * @param res
     * @return user we could establish
     * @throws NotPermitted
     * @throws IOException
     */
    public SecurityActor authenticate(Request req) throws IOException, NotAuthentic{
        // must recover user from cookies or by redirecting to login
        log().info("User is missing.");
        String auth=req.getCookie(KEY_NAME,null);
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
        return null;
    }
    /** will establish what if any rights an actor has on a securable. */
    public SecurityPermit authorize(SecurityActor actor, Securable subject){
        return actor.getPermit(subject);
    }
    /** loads a securable by id given actor permits. */
    public Securable loadSecurable(SecurityActor actor, Integer id) throws IOException, NotPermitted{
        return null;
    }
    /** loads an actor given name and password. */
    public SecurityActor loadActor(SecurityActor actor, String name, String pwd) throws IOException, NotPermitted{
        return null;
    }
    /** will save a securable including a user if permitted via actor. */
    public void saveSecurable(SecurityActor actor, Securable sec) throws IOException{

    }

}
