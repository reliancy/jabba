package com.reliancy.util;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;

/**
 * We will manage template rendering thru this class.
 */
public class Template {
    static Jinjava jinjava;
    static{
        jinjava = new Jinjava();
        jinjava.setResourceLocator(new JinjaLoader());
    }
    public static class JinjaLoader implements ResourceLocator{
        @Override
        public String getString(String fullName, Charset encoding, JinjavaInterpreter interpreter) throws IOException {
            URL loc=Resources.findFirst(null,fullName,Template.search_path);
            if(loc==null){
                Logger.getLogger(Template.class.getSimpleName()).warning("Missing template"+fullName);
                return "";
            }else{
                return Resources.toString(loc,encoding);
            }
        }
    }
    static Object[] search_path;
    static HashMap<String,Template> cache=new HashMap<>();
    /** renders a template to string, possibly locates it first.
     * 
     * @param path
     * @param context
     * @return
     * @throws IOException
     */
    public static CharSequence render(String path,Map<String,?> context) throws IOException{
        Template t=find(path,search_path);
        if(t==null){
            return null;
        }else{
            return t.render(context);
        }
    }
    /** returns a template based on a URL located over a search path.
     * 
     */
    public static Template find(String path,Object ... sp) {
        Template ret=cache.get(path);
        if(ret!=null) return ret;
        URL loc=Resources.findFirst(null, path, (sp!=null && sp.length>0?sp:search_path));
        System.out.println("TLOCL:"+loc);
        if(loc==null) return null;
        ret=new Template(loc);
        cache.put(path,ret);
        return ret;
    }
    public static Object[] search_path(Object...sp){
        if(sp!=null && sp.length>0) search_path=sp;
        return search_path;
    }

    final URL location;
    String source;
    public Template(URL location){
        this.location=location;
    }
    public Template(String src){
        this.location=null;
        this.source=src;
    }
    public URL getLocation(){
        return location;
    }
    public String getSource(){
        return source;
    }
    public Template load() throws IOException{
        if(source==null) this.source=Resources.toString(location);
        return this;
    }
    public CharSequence render(Map<String,?> context) throws IOException{
        if(source==null) load();
        String ret = jinjava.render(source, context);
        return ret;
    }
}
