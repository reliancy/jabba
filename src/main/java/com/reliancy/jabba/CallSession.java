package com.reliancy.jabba;

import java.util.ArrayList;

/**
 * Thread local object that lets us access some variables in specialized handler methods.
 * For example request and response objects are accessible.
 * The session is updated at process phase of each processor. 
 */
public class CallSession implements Session{
    ArrayList<Processor> callers=new ArrayList<>();
    Session appSession;
    Request request;
    Response response;
    
    public CallSession(){
    }
    protected void end(){
        appSession=null;
        request=null;
        response=null;
        callers.clear();
    }
    protected void begin(Session ss,Request req,Response resp){
        appSession=ss;
        request=req;
        response=resp;
        callers.clear();
    }
    protected void enter(Processor c){callers.add(c);}
    protected void leave(Processor c){
        int len=callers.size();
        int at=len-1;
        Processor last=len>0?callers.get(at):null;
        if(c!=null && c==last){
            callers.remove(len-1);
        }else if(len>0 && (at=callers.indexOf(c))!=-1){
            // bad last is not same c, some processors have not left properly
            do{
                last=callers.remove(callers.size()-1);
            }while(last!=c);
        }
    }
    @Override
    public void setValue(String key, Object val) {
        if(appSession!=null) appSession.setValue(key, val);
    }
    @Override
    public Object getValue(String key) {
        return appSession!=null?appSession.getValue(key):null;
    }
    public void setAppSession(Session ss) {
        appSession=ss;
    }
    public Session getAppSession() {
        return appSession;
    }
    public Request getRequest() {
        return request;
    }
    public void setRequest(Request request) {
        this.request = request;
    }
    public Response getResponse() {
        return response;
    }
    public void setResponse(Response response) {
        this.response = response;
    }
    public Processor getCaller() {
        int len=callers.size();
        return len>0?callers.get(len-1):null;
    }
    /**
     * Will return current session given the call stack.
     * @return
     */
    public static ThreadLocal<CallSession> instance=new ThreadLocal<>();
    public static CallSession getInstance(){
        CallSession ret=instance.get();
        if(ret==null) instance.set(ret=new CallSession());
        return ret;
    }
}
