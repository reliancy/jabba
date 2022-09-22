/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RoutedEndPoint extends EndPoint{
    HashMap<String,EndPoint> routes=new HashMap<>();      // route pattern to endpoint
    ArrayList<RouteDetector> detectors=new ArrayList<>(); // route patterns ordered
    int[] indexes;              // indexes for each route within regex
    Pattern regex;

    public RoutedEndPoint() {
        super("router");
    }

    @Override
    public void serve(Request req, Response resp) throws IOException {
        //System.out.println(req.http_request);
        String verb=req.getVerb();
        String path=req.getPath();
        log().info("serving:{}",path);
        Matcher m=match(verb,path);
        //Matcher m=rep.match("GET","/helloP");
        if(m!=null){
            //HashMap<String,String> pms=new HashMap<>();
            String rt=evalMatcher(m,req.getPathParams());
            //System.out.println(req.getPathParams());
            EndPoint ep=getRoute(rt);
            if(ep!=null){
                ep.process(req, resp);
            }else{
                log().error("no endpoint for:{}",rt);
                resp.setContentType("text/plain;charset=utf-8");
                resp.setStatus(Response.HTTP_NOT_FOUND);
                resp.getEncoder().writeln("no endpoint for :"+rt);
            }
        }else{
            log().error("could not resolve path:{}",path);
            resp.setContentType("text/plain;charset=utf-8");
            resp.setStatus(Response.HTTP_NOT_FOUND);
            resp.getEncoder().writeln("could not resolve path:"+path);
        }
    }
    public EndPoint getRoute(String r){
        return routes.get(r);
    }
    public void addRoute(String verb,String path, EndPoint mm) {
        RouteDetector det=new RouteDetector(verb,path);
        detectors.add(det);
        routes.put(det.getPattern(),mm);
    }

    public void compile() {
        // sort with longest first
        Collections.sort(detectors,Comparator.comparing((det)->{return -det.getPath().length();}));
        String fullPat=detectors
            .stream()
            .map(RouteDetector::toString)
            .collect(Collectors.joining(")|("));
        fullPat = "("+fullPat+")";
        //System.out.println("FUll:"+fullPat);
        regex=Pattern.compile(fullPat);
        // also recompute indexes
        indexes=new int[detectors.size()];
        int index=1;
        for (int i = 0; i < indexes.length; i++) {
            indexes[i]=index;
            RouteDetector det=detectors.get(i);
            index+=2; // this includes the verb group
            if(det.hasParams()){ // this includes any param groups
                index+=det.getParams().size();
            }
        }
        //Arrays.stream(indexes).forEach(e->System.out.println(e+" "));
    }
    public Matcher match(String verb,String path){
        if(regex==null) compile();
        String input=verb+" "+path;
        Matcher m=regex.matcher(input);
        if(!m.find()) return null;
        return m;
    }
    /**
     * Find the route and return also url params.
     * url params are saved in two ways by name and by pos.
     * @param m matcher to check
     * @param p parameters to reference
     * @return
     */
    public String evalMatcher(Matcher m,Map<String,String> p){
        String gstr=null;
        int gindex=1;
        while(gindex<m.groupCount() && gstr==null){
            gstr=m.group(gindex);
            if(gstr==null) gindex++;
        }
        if(gstr==null) return null; // no group found
        // now find route given group
        int rindex=-1;
        for (int i = 0; i < indexes.length && rindex<0; i++) {
           if(gindex==indexes[i]) rindex=i;
        }
        if(rindex<0) return null; // we can't match route to group
        RouteDetector det=detectors.get(rindex);
        //String ret=patterns.get(rindex);
        if(p!=null && det.hasParams()){
            ArrayList<String> pms=det.getParams();
            for(int i=0;i<pms.size();i++){
                String val=m.group(gindex+2+i);
                String byName=pms.get(i).toLowerCase();
                p.put(byName,val);
                p.put("_arg"+i,val);
            }
        }
        return det.getPattern();
    }
    /**
     * Will import endpoints to serve various paths.
     * We can call this multiple times for multiple targets.
     * @param target
     * @return
     */
    public RoutedEndPoint importMethods(Object target){
        //RoutedEndPoint ret=new RoutedEndPoint();
        LinkedList<Method> routes=new LinkedList<>();
        Class<?> type=target.getClass();
        while (type != null) {
            for(Method m : type.getDeclaredMethods()){
                //System.out.println("Method:"+m.toString());
                if(m.getAnnotation(Routed.class)!=null){
                    routes.add(0,m);
                }
            }
            type = type.getSuperclass();
        }
        for(Method m:routes){
            MethodEndPoint mm=new MethodEndPoint(target,m);
            addRoute(mm.getVerb(),mm.getPath(),mm);
        }
        return this;
    }
}
