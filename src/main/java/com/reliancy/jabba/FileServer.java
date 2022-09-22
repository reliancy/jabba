/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import com.reliancy.util.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Stream;

public class FileServer extends EndPoint implements Resources.PathRewrite{
    public static interface Filter{
        boolean isAcceptable(String path);
    }
    public static class ExtFilter implements Filter{
        final String[] allowed;
        public ExtFilter(String ...ext){allowed=ext;}
        @Override
        public boolean isAcceptable(String path) {
            if(allowed.length==0) return true;
            for(String ext:allowed) if(path.endsWith(ext)) return true;
            return false;
        }
    }
    public static Filter NOFILTER=new ExtFilter();
    String diskPrefix;
    String classPrefix;
    final HashMap<String,Object[]> map;
    final HashMap<String,Filter> filt;
    public FileServer(String url_path,Filter f,Object ... disk_path){
        super("fileserver");
        diskPrefix=classPrefix=null;
        filt=new HashMap<>();
        map=new HashMap<>();
        addRoute(url_path,f, disk_path);
    }
    public FileServer(String url_path,Object ... disk_path){
        this(url_path,NOFILTER,disk_path);
    }
    @Override
    public void serve(Request request, Response response) throws IOException {
        String path=request.getPath();
        log().debug("to serve:"+path);
        for(String prefix:map.keySet()){
            boolean match=path.startsWith(prefix);
            if(match){
                Object[] sp=getSearchPath(prefix);
                String rpath=path.replace(prefix,"");
                if(!filt.get(prefix).isAcceptable(rpath)) continue; // not acceptable to filter
                URL f=Resources.findFirst(this, rpath, sp);
                if(f==null) continue; // skip if rpath not located
                this.log().debug("\tfound:"+f);
                writeResource(f,response);
                return;
            }
        }
        response.setStatus(Response.HTTP_NOT_FOUND);
        response.getEncoder().writeln("missing file:{0}",path);
        this.log().error("not found:"+path);
    }
    /**
     * we prefix our path for disk and class contexts.
     */
    @Override
    public String rewritePath(String path, Object context) {
        if(diskPrefix!=null && context instanceof String) return this.diskPrefix+path;
        if(diskPrefix!=null && context instanceof File) return this.diskPrefix+path;
        if(classPrefix!=null && context instanceof Class) return this.classPrefix+path;
        return path;
    }
    public FileServer setDiskPrefix(String prefix){
        diskPrefix=prefix;
        return this;
    }
    public FileServer setClassPrefix(String prefix){
        classPrefix=prefix;
        return this;
    }
    /**
     * Will render a file to response.
     * @param f
     * @param response
     */
    protected void writeResource(URL f, Response response) throws IOException{
        //log().info("writing:"+f);
        ResponseEncoder enc=response.getEncoder();
        try(InputStream is=f.openStream()){
            String ctype=HTTP.guess_mime(f);
            response.setStatus(Response.HTTP_OK);
            response.setContentType(ctype);
            enc.writeStream(is);
        }
    }
    public final void addRoute(String url_path,Filter f,Object... disk_path){
        if(disk_path!=null){
            map.put(url_path,disk_path);
            filt.put(url_path,f!=null?f:NOFILTER);
        }else{
            map.remove(url_path);
            filt.remove(url_path);
        }
    }
    public Object[] getSearchPath(String url_path){
        return map.get(url_path);
    }
    public Filter getFilter(String url_path){
        return filt.get(url_path);
    }
    public Stream<String> streamRoutes() {
        return map.keySet().stream();
    }
    public Iterator<String> enumRoutes(){
        return map.keySet().iterator();
    }
    public void exportRoutes(RoutedEndPoint rep) {
        streamRoutes().forEach(up->rep.addRoute("GET",up+".*",this));
    }
}
