/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;
import com.reliancy.util.Handy;
import com.reliancy.util.LRUCache;
import com.reliancy.util.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.Logger;

/** FileServer is an module and endpoint that exposes multiple URLs thru which files are served.
 * First it will be just get(tting), later 
 * TODO: putting, posting and maybe full DAV.
 * TODO: We will need proper security.
 * TODO: We will also add in memory serving.
 * We have added cache control and etag support.
 * Please note Router is for routing. 
 * Bucket is there to process input/output given verbs over resources under it.
 */
public class FileServer extends EndPoint implements AppModule,Resources.PathRewrite{
    /** Bucket interface to abstract i/o and provide easier extensibility. 
     * asContainer matches path and then returns local-to-packet path.
     * signature returns a hash over lastModified or content that reflects modification.
    */
    public static interface Bucket{
        String getPrefix();
        String asContained(String path);
        boolean equals(String pref);
        InputStream openSource(String local_path,FileServer user) throws IOException;
        OutputStream openSink(String local_path,FileServer user) throws IOException;
        String signature(String local_path);
    }
    public static class FileBucket implements Bucket{
        final String prefix;
        String[] extAllowed;
        Object[] domain;
        LRUCache<String,Long> hit_history=new LRUCache<>(2*Runtime.getRuntime().availableProcessors());

        public FileBucket(String prefix){
            this.prefix=prefix;
            extAllowed=new String[]{};
            domain=new Object[]{};
        }
        @Override
        public final String getPrefix(){return prefix;}
        @Override
        public String asContained(String path) {
            if(!path.startsWith(prefix)) return null; // not contained
            String local_path=path.replace(prefix,"");
            if(extAllowed.length==0) return local_path;
            for(String ext:extAllowed){
                if(path.endsWith(ext)){
                    return local_path;
                }
            }
            return null;
        }
        @Override
        public boolean equals(Object o){
            if(o instanceof FileBucket) return prefix.equals(((FileBucket)o).getPrefix());
            if(o instanceof String) return prefix.equals((String)o);
            return false;
        }
        @Override
        public boolean equals(String pref){return prefix.equals(pref);}
        public FileBucket setDomain(Object...sp){
            domain=sp;
            return this;
        }
        public Object[] getDomain(){
            return (domain!=null && domain.length>0)?domain:Resources.search_path;
        }
        public InputStream openSource(String local_path,FileServer user) throws IOException{
            Object[] sp=getDomain();
            URL f=Resources.findFirst(user,local_path, sp);
            if(f==null) return null; // skip if rpath not located
            URLConnection conn=f.openConnection();
            hit_history.put(local_path,conn.getLastModified()); // pull last modified for signature
            return conn.getInputStream();
        }
        public OutputStream openSink(String local_path,FileServer user) throws IOException{
            return null;
        }
        public String signature(String local_path){
            Long last_modified=hit_history.get(local_path);
            if(last_modified==null) return null;
            String sig=String.valueOf(last_modified);
            return Handy.hashMD5(sig);
        }
    }
    final ArrayList<Bucket> buckets=new ArrayList<>();
    String diskPrefix;      // will be prefixed to source if file
    String classPrefix;     // will be prefixed to source if class
    String urlPrefix;       // will be prefixed to source if URL
    public FileServer(String url_path,String offset,Object ... source){
        super(null);
        diskPrefix=classPrefix=offset;
        addBucket(new FileBucket(url_path).setDomain(source));
    }
    public FileServer(){
        super(null);
    }
    public FileServer setDiskPrefix(String prefix){
        diskPrefix=prefix;
        return this;
    }
    public FileServer setClassPrefix(String prefix){
        classPrefix=prefix;
        return this;
    }
    public FileServer setURLOffset(String offset){
        urlPrefix=offset;
        return this;
    }
    /**
     * we prefix our path for disk and class contexts.
     */
    @Override
    public String rewritePath(String path, Object context) {
        if(diskPrefix!=null && context instanceof String) return this.diskPrefix+path;
        if(diskPrefix!=null && context instanceof File) return this.diskPrefix+path;
        if(classPrefix!=null && context instanceof Class) return this.classPrefix+path;
        if(urlPrefix!=null && context instanceof URL) return this.urlPrefix+path;
        return path;
    }
    @Override
    public void serve(Request request, Response response) throws IOException {
        String verb=request.getVerb();
        String path=request.getPath();
        Logger logger=log();
        boolean atDebug=logger.isDebugEnabled();
        if(atDebug) logger.debug("{}:{}",verb,path);
        if(HTTP.VERB_GET.equals(verb)){
            for(Bucket bucket:buckets){
                String local_path=bucket.asContained(path);
                if(local_path==null) continue; // this bucket is not accepting
                try(InputStream ins=bucket.openSource(local_path,this)){
                    if(ins==null) continue; // url did not take
                    String etag=bucket.signature(local_path);
                    if(etag!=null){
                        response.setHeader("Cache-Control","max-age=0, must-revalidate");
                        response.setHeader("ETag",etag);
                        String etag_old=request.getHeader("If-None-Match");
                        if(etag.equals(etag_old)){
                            // we got same etag no change
                            response.setStatus(Response.HTTP_NOT_MODIFIED);
                            return;
                        }
                    }
                    if(atDebug) logger.debug("\tfound:"+local_path);
                    String ctype=HTTP.ext2mime(local_path);
                    response.setStatus(Response.HTTP_OK);
                    response.setContentType(ctype);
                    ResponseEncoder enc=response.getEncoder();
                    enc.writeStream(ins);
                    return; // we got something
                }
            }
        }else{
            // these verbs are not supported
        }
        response.setStatus(Response.HTTP_NOT_FOUND);
        response.getEncoder().writeln("missing file:"+path);
        logger.error("not found:{}",path);
    }
    /**
     * Will render a URL resource to response.
     * @param f
     * @param response
     */
    public static boolean sendData(URL f, Response response) throws IOException{
        try(InputStream is=f.openStream()){
            if(is==null) return false;
            response.setStatus(Response.HTTP_OK);
            String ctype=HTTP.guess_mime(f);
            response.setContentType(ctype);
            ResponseEncoder enc=response.getEncoder();
            enc.writeStream(is);
            return true;
        }
    }
    public static boolean sendData(InputStream istr, Response response) throws IOException{
        if(istr==null) return false;
        ResponseEncoder enc=response.getEncoder();
        response.setStatus(Response.HTTP_OK);
        enc.writeStream(istr);
        return true;
    }
        /** adds a route which serves files.
     * if disk_path is ommited (0 len) or null we use Resources.search_path.
     * @param bucket resource holder to add 
     */
    public final FileServer addBucket(Bucket bucket){
        buckets.add(bucket);
        return this;
    }
    public FileServer delBucket(Bucket bucket){
        buckets.remove(bucket);
        return this;
    }
    public Bucket getBucket(String url_path){
        for(Bucket b:buckets) if(b.equals(url_path)) return b;
        return null;
    }
    public Iterator<Bucket> enumBuckets(){
        return buckets.iterator();
    }
    public void publish(App app) {
        Router rep=app.getRouter();
        for(Bucket b:buckets) rep.addRoute("GET",b.getPrefix()+".*",this);
    }
}
