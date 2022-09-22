/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import com.reliancy.util.Handy;

public class MethodEndPoint extends EndPoint{
    enum InvokeProfile{
        PLAIN,  // no return, request, response as argument
        NOARG,  // no arguments, possible return
        FULL,   // one or more arguments need to do casting

    }    
    Routed route;
    Object target;
    Method method;
    Parameter[] params;
    Class<?> retType;
    InvokeProfile invokeType;
    ArrayList<MethodDecorator> decorators=new ArrayList<>();
    
    public MethodEndPoint(Object target,Method m) {
        super(target.getClass().getSimpleName()+"."+m.getName());
        this.route=m.getAnnotation(Routed.class);
        this.target=target;
        this.method=m;
        this.params=m.getParameters();
        this.retType=m.getReturnType();
        this.invokeType=InvokeProfile.FULL;
        if(params.length==2 && params[0].getType()==Request.class && params[1].getType()==Response.class){
            invokeType=InvokeProfile.PLAIN;
        }
        if(params.length==0){
            invokeType=InvokeProfile.NOARG;
        }
        bindDecorators();
    }
    public String getVerb(){
        return route.verb();
    }
    public String getPath() {
        String ret=route.path();
        if(!ret.startsWith("/")) ret="/"+ret;
        ret=ret.replace("{method}",method.getName());
        return ret;
    }
    public Routed getRoute(){
        return route;
    }
    /** pulls in and adds decorator filters to this methodcall that are supported. */
    protected final void bindDecorators(){
        for(Annotation a:method.getAnnotations()){
            MethodDecorator d=MethodDecorator.query(this,a);
            if(d!=null) decorators.add(d);
        }
    }
    @Override
    public void serve(Request request, Response response) throws IOException{
        log().info("Serving method....{}",invokeType);
        try{
            Object ret=null;
            switch(invokeType){
                case PLAIN:{ // plain profile just passes req,resp
                    method.invoke(target,request,response);
                    break;
                }
                case NOARG:{ // no args will not pass any arguments, will deal with return (marshalling)
                    ret=method.invoke(target);
                    encodeResponse(ret,response);
                    break;
                }
                default:{   // here we do full unmarshalling, marshalling
                    Object[] argVals=decodeRequest(request);
                    ret=method.invoke(target,argVals);
                    encodeResponse(ret,response);
                }
            }
        }catch(Exception ex2){
            if(ex2 instanceof IOException) throw ((IOException)ex2);
            else throw new IOException(ex2);
        }
    }
    protected Object[] decodeRequest(Request request){
        Object[] argVals=new Object[params.length];
        for(int i=0;i<argVals.length;i++){
            Parameter p=params[i];
            Class<?> cls=p.getType();
            String byName=p.getName();
            String byPos="_arg"+i;
            Object val=request.getParam(byName,request.getParam(byPos,null)); // get by name or pos
            argVals[i]=Handy.normalize(cls,val);
        }
        return argVals;
    }
    protected void encodeResponse(Object ret, Response response) throws IOException{
        if(ret instanceof Response){
            // we have a response return  - take its status and content type
            Response resp=(Response)ret;
            if(resp!=response){
                response.setStatus(resp.getStatus());
                response.setContentType(resp.getContentType());
                resp.exportContent(response.getEncoder());
            }
        }else{
            // we do not have a response but must set status, content type
            String ctype=route.return_mime();
            if(Handy.isBlank(ctype)) ctype=HTTP.guess_mime(ret);
            response.setContentType(ctype);
            if(ret!=null){
                response.getEncoder().writeObject(ret);
            }
        }
    }
}
