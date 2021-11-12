package com.reliancy.jabba;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

/** HTTP related methods and classes. */
public final class HTTP {
    public static HashMap<String,String> MIME_MAP=new HashMap<>();
    public static class Header{
        public String key;
        public String value;
        public Header(String k, String v){
            key=k;value=v;
        }
    }
    public static class Cookie{
        public String key;
        public String value;
        public int maxAge;
        public boolean secure;
        public Cookie(String k,String v, int maxAge, boolean sec){
            key=k;value=v;this.maxAge=maxAge;secure=sec;
        }
    }
    public static String ext2mime(String ext){
        if(MIME_MAP.isEmpty()){
            MIME_MAP.put("ico","image/x-icon");
            MIME_MAP.put("js","application/javascript");
            MIME_MAP.put("doc","application/msword");
            MIME_MAP.put("pdf","application/pdf");
            MIME_MAP.put("zip","application/zip");
            MIME_MAP.put("gif","image/gif");
            MIME_MAP.put("jpg","image/jpeg");
            MIME_MAP.put("jpeg","image/jpeg");
            MIME_MAP.put("png","image/png");
            MIME_MAP.put("webp","image/webp");
            MIME_MAP.put("txt","text/plain");
            MIME_MAP.put("css","text/css");
            MIME_MAP.put("csv","text/csv");
            MIME_MAP.put("html","text/html"); 
            MIME_MAP.put("htm","text/html");
            MIME_MAP.put("xml","text/xml");
        }
        return MIME_MAP.get(ext);
    }
    public static String guess_mime(Object ret) {
        if(ret instanceof CharSequence){
            CharSequence retstr=(CharSequence)ret;
            for(int index=0;index<retstr.length();index++){
                char ch=retstr.charAt(index);
                if(Character.isWhitespace(ch)) continue;
                if(ch=='<') return "text/html";
                if(ch=='{' || ch=='[') return "application/json";
                break;
            }
            return "text/plain";
        }
        if(ret instanceof byte[]){
            return "application/octet-stream";
        }
        if(ret instanceof File || ret instanceof URL){
            String path=String.valueOf(ret);
            String ext=path.substring(path.lastIndexOf(".")+1).toLowerCase();
            String mime=ext2mime(ext);
            return mime!=null?mime:"application/octet-stream";
        }
        return null;
    }
}
