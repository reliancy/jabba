package com.reliancy.jabba;

import java.io.IOException;
import java.util.UUID;

/** AppSession middleware will inject an appsession object into callsession.
 * During each request,response we will if not alrady present extract a cookie or param
 * and based on it install an app wide sesson dictionary.
 */
public class AppSessionFilter extends Processor{
    public static final String KEY_NAME="jbssid";
    public AppSessionFilter() {
        super(AppSessionFilter.class.getSimpleName().toLowerCase());
    }
    @Override
    public void before(Request request, Response response) throws IOException {
        String ssid=(String)request.getParam(KEY_NAME,null);
        if(ssid==null){
            UUID uuid = UUID.randomUUID();
            ssid=uuid.toString();
        }
        AppSession ss=AppSession.getInstance(ssid);
        if(ss!=null){
            if(ss.isExpired()){
                // this app sessin expired - create a new one
                ss=new AppSession(ssid);
                AppSession.setInstance(ssid, ss);
            }else{
                // this session is good
                ss.setLastActive();
            }
        }else{
            // no session available
            ss=new AppSession(ssid);
            AppSession.setInstance(ssid, ss);
        }
        CallSession css=CallSession.getInstance();
        css.setAppSession(ss);
    }
    @Override
    public void after(Request request, Response response) throws IOException {
        CallSession css=CallSession.getInstance();
        AppSession ss=(AppSession) css.getAppSession();
        response.setCookie(KEY_NAME,ss.id,15*60,false);
    }
    @Override
    public void serve(Request request, Response response) throws IOException{

    }
}
