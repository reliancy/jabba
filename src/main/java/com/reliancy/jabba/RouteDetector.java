/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.reliancy.util.Handy;

/** Utility class that hosts and matches routes.
 * This object is needed so we can support parametrized paths.
 * It will match a route but also extract parameters.
 */
public class RouteDetector {
    String verb;
    String path;
    String pattern;
    final ArrayList<String> params=new ArrayList<String>();
    Pattern regex;
    Object payload;

    public RouteDetector(String verb,String path){
        this.verb=verb;
        this.path=path;
        if(this.verb==null) this.verb="GET|POST|DELETE";
        verb=verb.toUpperCase();
        pattern=toPattern(verb, path);
        regex=Pattern.compile(pattern);
        Pattern p=Pattern.compile("\\{(.+)\\}");
        Matcher m=p.matcher(path);
        while(m.find()){
            String g=m.group();
            params.add(Handy.unwrap(g,"{","}"));
        }
        //if(params.isEmpty()==false) routeParams.put(routePat,params);
    }
    public String toString(){
        return getPattern();
    }
    public String getPattern(){
        return pattern;
    }
    public String getVerb(){
        return verb;
    }
    public String getPath(){
        return path;
    }
    public static String toPattern(String verb,String path){
        String pathPattern=path.replaceAll("\\{(.+)\\}","(.+)");
        String ret=Handy.wrap(verb,"(",")")+" "+pathPattern;
        if(!ret.endsWith("/") && !ret.endsWith("$")) ret+="$";
        return ret;
    }
    public boolean hasParams(){
        return !params.isEmpty();
    }
    public ArrayList<String> getParams(){
        return params;
    }
    public boolean matches(String pat){
        return matches(pat,null);
    }
    public boolean matches(String pat,Map<String,String> p){
        Matcher m=regex.matcher(pat);
        if(m.find()){ // do we match
            // we do - now possibly extract params
            if(p!=null){
                ArrayList<String> pms=getParams();
                for(int i=0;i<pms.size();i++){
                    String val=m.group(1+i);
                    String byName=pms.get(i).toLowerCase();
                    p.put(byName,val);
                    p.put("_arg"+i,val);
                }
            }
            return true;
        }
        return false;
    }
    @Override
    public int hashCode(){
        return getPattern().hashCode();
    }
    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        }
        if (o == this){
            return true;
        }
        if (o instanceof RouteDetector && getPattern().equals(((RouteDetector)o).getPattern())){
            return true;
        }
        if( o instanceof String) return matches((String)o,null);
        return false;
    }
    public RouteDetector setPayload(Object val){
        payload=val;
        return this;
    }
    public Object getPayload(){
        return payload;
    }
}
