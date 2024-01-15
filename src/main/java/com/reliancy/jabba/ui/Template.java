/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.github.jknack.handlebars.io.URLTemplateSource;
import com.reliancy.util.Resources;
import com.reliancy.util.ResultCode;

/*
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;
*/

/**
 * We will manage template rendering thru this class.
 */
public class Template {
    /*
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
    */
    public static class HBLoader extends AbstractTemplateLoader{
        public HBLoader(){
            this.setPrefix("/templates/");
        }
        @Override
        public TemplateSource sourceAt(String location) throws IOException {
            String fullpath=this.resolve(location);
            URL loc=Resources.findFirst(null,fullpath,Template.search_path());
            //System.out.println(location+":"+loc+":"+fullpath);
            if (loc == null) {
                Logger.getLogger(Template.class.getSimpleName()).warning("Template missing:"+fullpath);
                throw new FileNotFoundException(location);
            }
            return new URLTemplateSource(location,loc);            
        }
        public String resolve(String uri){
            if(uri==null || uri.isEmpty()) return uri;
            // we strip prefix and suffic just in case
            if(uri.startsWith(this.getPrefix())) uri=uri.substring(this.getPrefix().length());
            if(uri.endsWith(this.getSuffix())) uri=uri.substring(0,uri.indexOf(this.getSuffix()));
            if(Template.partial_map.containsKey(uri)){
                uri=Template.partial_map.get(uri);
            }
            return super.resolve(uri);
        }
    }
    static Handlebars handlebars;
    static HashMap<String,String> partial_map=new HashMap<>();
    static Object[] search_path;
    static HashMap<String,Template> cache=new HashMap<>();
    static{
        handlebars= new Handlebars(new HBLoader());
        StringHelpers.register(handlebars);
        /* 
        for(ConditionalHelpers h:ConditionalHelpers.values()){

        }
        */
        partial_map.put("__frame__","frame-land");
    }
    /** renders a template to string, possibly locates it first.
     * 
     * @param path
     * @param context
     * @return
     * @throws IOException
     */
    public static CharSequence render(String path,Map<String,?> context) throws IOException{
        Template t=find(path);
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
        URL loc=Resources.findFirst(null, path, (sp!=null && sp.length>0?sp:search_path()));
        if(loc==null) return null;
        ret=new Template(loc);
        cache.put(path,ret);
        return ret;
    }
    public static Object[] search_path(Object...sp){
        if(sp!=null && sp.length>0) search_path=sp;
        return search_path!=null?search_path:Resources.search_path;
    }
    /**
     * will register a partial mapping to let us dynamically switch partials.
     * this is useful in writing components for various layouts.
     * all components derive from frame while frame is switched to frame-dash or frame-land.
     * @param src
     * @param dst
     */
    public static void remap_partial(String src,String dst){
        Template.partial_map.put(src,dst);
    }
    public static final int ERR_BADTEMPLATE=ResultCode.defineFailure(0x01,Template.class,"bad template: ${template}");
    com.github.jknack.handlebars.Template recipe;
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
    public CharSequence render(Object context) throws IOException{
        if(source==null) load();
        //String ret = jinjava.render(source, context);
        if(recipe==null){
            recipe=handlebars.compileInline(source);
        }
        return recipe.apply(context);
    }
    public void render(Object context,Writer _out) throws IOException{
        if(source==null) load();
        //String ret = jinjava.render(source, context);
        if(recipe==null){
            recipe=handlebars.compileInline(source);
        }
        recipe.apply(context,_out);
    }
}
