/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

/** HTTP related methods and classes. 
 * 
*/
public final class HTTP {
    public static String VERB_GET="GET";
    public static String VERB_PUT="PUT";
    public static String VERB_DEL="DELETE";
    public static String VERB_POST="POST";
    public static String VERB_HEAD="HEAD";
    
    public static String MIME_PLAIN="text/plain";
    public static String MIME_JSON="application/json";
    public static String MIME_BYTES="application/octet-stream";
    public static String MIME_HTML="text/html";

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
    /** maps extension to mime type.
     * it will extract everything after last dot so we can pass a path too.
     * @param ext
     * @return
     */
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
            MIME_MAP.put("txt",MIME_PLAIN);
            MIME_MAP.put("css","text/css");
            MIME_MAP.put("csv","text/csv");
            MIME_MAP.put("html",MIME_HTML); 
            MIME_MAP.put("htm",MIME_HTML);
            MIME_MAP.put("xml","text/xml");
            MIME_MAP.put("json",MIME_JSON);
        }
        ext=ext.substring(ext.lastIndexOf(".")+1).toLowerCase();
        return MIME_MAP.get(ext);
    }
    /** guesses mime type based on content.
     * for url,path or file looks at the path otherwise it examines content.
     * if you pass in charsequence it will look for indicators of html or json.
     * for bytes nothing yet but we could examine headers to images.
     * @param ret
     * @return
     */
    public static String guess_mime(Object ret) {
        if(ret instanceof CharSequence){
            CharSequence retstr=(CharSequence)ret;
            for(int index=0;index<retstr.length();index++){
                char ch=retstr.charAt(index);
                if(Character.isWhitespace(ch)) continue;
                if(ch=='<') return MIME_HTML;
                if(ch=='{' || ch=='[') return MIME_JSON;
                break;
            }
            return MIME_PLAIN;
        }
        if(ret instanceof byte[]){
            return MIME_BYTES;
        }
        if(ret instanceof File || ret instanceof URL || ret instanceof Path){
            String path=String.valueOf(ret);
            String ext=path.substring(path.lastIndexOf(".")+1).toLowerCase();
            String mime=ext2mime(ext);
            return mime!=null?mime:MIME_BYTES;
        }
        return null;
    }
}
