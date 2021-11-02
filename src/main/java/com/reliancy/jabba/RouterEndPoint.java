package com.reliancy.jabba;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.reliancy.util.Handy;

public class RouterEndPoint extends EndPoint{
    HashMap<String,EndPoint> routes=new HashMap<>();
    HashMap<String,ArrayList<String>> routeParams=new HashMap<>();
    ArrayList<String> patterns=new ArrayList<>(); // route patterns ordered
    int[] indexes;              // indexes for each route within regex
    Pattern regex;

    public RouterEndPoint() {
        super(null);
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
            //System.out.println(rt);
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
        if(verb==null) verb="GET|POST|DELETE";
        String pathPat=path.replaceAll("\\{(.+)\\}","(.+)");
        String routePat=Handy.wrap(verb,"(",")")+" "+pathPat;
        if(!routePat.endsWith("/") && !routePat.endsWith("$")) routePat+="$";
        routes.put(routePat,mm);
        //System.out.println("Adding route:"+routePat);
        ArrayList<String> params=new ArrayList<String>();
        Pattern p=Pattern.compile("\\{(.+)\\}");
        Matcher m=p.matcher(path);
        while(m.find()){
            String g=m.group();
            params.add(Handy.unwrap(g,"{","}"));
        }
        if(params.isEmpty()==false) routeParams.put(routePat,params);
    }

    public void compile() {
        patterns.clear();
        for(String r:routes.keySet()){
            patterns.add(r);
        }
        // sort with longest first
        Collections.sort(patterns,Comparator.comparing((str)->{return -str.length();}));
        String fullPat = "("+String.join(")|(",patterns)+")";
        regex=Pattern.compile(fullPat);
        // also recompute indexes
        indexes=new int[patterns.size()];
        int index=1;
        for (int i = 0; i < indexes.length; i++) {
            indexes[i]=index;
            String p=patterns.get(i);
            index+=2; // this includes the verb group
            if(routeParams.containsKey(p)){ // this includes any param groups
                index+=routeParams.get(p).size();
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
     * @param m
     * @param routeParams
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
        String ret=patterns.get(rindex);
        if(p!=null && routeParams.containsKey(ret)){
            ArrayList<String> pms=routeParams.get(ret);
            for(int i=0;i<pms.size();i++){
                String val=m.group(gindex+2+i);
                String byName=pms.get(i).toLowerCase();
                p.put(byName,val);
                p.put("_arg"+i,val);
            }
        }
        return ret;
    }
}
