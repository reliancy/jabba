/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Our representation of the response.
 * We usually wrap servlet response with this object and use in write mode.
 * But we can also create it with no servletresponse then it represents delayed response to be 
 * read out later and written somewhere.
 */
public class Response {
    // status codes
    public static final int HTTP_OK=HttpServletResponse.SC_OK;
    public static final int HTTP_BAD_REQUEST=HttpServletResponse.SC_BAD_REQUEST;
    public static final int HTTP_NOT_FOUND=HttpServletResponse.SC_NOT_FOUND;
    public static final int HTTP_UNAUTHORIZED=HttpServletResponse.SC_UNAUTHORIZED;
    public static final int HTTP_FORBIDDEN=HttpServletResponse.SC_FORBIDDEN;
    public static final int HTTP_TEMPORARY_REDIRECT=HttpServletResponse.SC_TEMPORARY_REDIRECT;
    public static final int HTTP_FOUND_REDIRECT=HttpServletResponse.SC_FOUND;
    public static final int HTTP_NOT_MODIFIED=HttpServletResponse.SC_NOT_MODIFIED;
    public static final int HTTP_INTERNAL_ERROR=HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    
    final protected HttpServletResponse  http_response;
    final protected Writer char_response;
    final protected OutputStream byte_response;
    protected ResponseEncoder encoder;
    protected String content_type;
    protected Integer status;
    protected final ArrayList<HTTP.Header> headers=new ArrayList<>();
    protected final ArrayList<HTTP.Cookie> cookies=new ArrayList<>();

    public Response(HttpServletResponse http_response) {
        this.http_response = http_response;
        this.char_response=null;
        this.byte_response=null;
    }
    public Response(Writer w) {
        this.http_response = null;
        this.char_response=w;
        this.byte_response=null;
    }
    public Response(OutputStream w) {
        this.http_response = null;
        this.char_response=null;
        this.byte_response=w;
    }
    public Response() {
        this.http_response = null;
        this.char_response=new StringWriter();
        this.byte_response=null;
    }
    public ResponseEncoder getEncoder(){
        if(encoder==null) encoder=new ResponseEncoder(this);
        return encoder;
    }
    /**returns accumulated string body content if in stringwriter mode or possibly bytearray*/
    public Object getContent(){
        if(char_response instanceof StringWriter){
            return ((StringWriter)char_response).toString();
        }else if( byte_response instanceof ByteArrayOutputStream){
            return ((ByteArrayOutputStream)byte_response).toByteArray();
        }else return null;
    }
    /** similar to get content only sends own content to external encoder. 
     * @throws IOException
     **/
    public void exportContent(ResponseEncoder ext) throws IOException {
        if(char_response instanceof StringWriter){
            ext.writeString(((StringWriter)char_response).toString());
        }else if( byte_response instanceof ByteArrayOutputStream){
            byte[] buf=((ByteArrayOutputStream)byte_response).toByteArray();
            ext.writeBytes(buf,0,buf.length);
        }
    }

    public void setContentType(String ctype) {
        content_type=ctype;
        if(http_response!=null) http_response.setContentType(ctype);
    }
    public String getContentType(){
        return content_type;
    }
    public void setStatus(int status) {
        this.status=status;
        if(http_response!=null) http_response.setStatus(status);
    }
    public Integer getStatus(){
        return status;
    }
    public String getHeader(String key){
        for(HTTP.Header hdr:headers){
            if(key.equalsIgnoreCase(key)) return hdr.value;
        }
        if(http_response!=null){
            return http_response.getHeader(key);
        }else{
            return null;
        }
    }
    public Response setHeader(String key,String val){
        HTTP.Header sel=null;
        for(HTTP.Header hdr:headers){
            if(key.equalsIgnoreCase(key)){
                sel=hdr;
                break;
            }
        }
        if(sel!=null) sel.value=val; else headers.add(new HTTP.Header(key,val));
        if(http_response!=null) http_response.setHeader(key,val);
        return this;
    }
    public List<HTTP.Header> getHeaders(){
        return headers;
    }
    public String getCookie(String key){
        for(HTTP.Cookie c:cookies){
            if(key.equalsIgnoreCase(key)) return c.value;
        }
        return null;
    }
    public Response setCookie(String key,String val,int maxAge,boolean secure){
        HTTP.Cookie sel=null;
        for(HTTP.Cookie hdr:cookies){
            if(key.equalsIgnoreCase(key)){
                sel=hdr;
                break;
            }
        }
        if(sel!=null){
            sel.value=val;
            sel.maxAge=maxAge;
            sel.secure=secure;
        } else{
            cookies.add(new HTTP.Cookie(key,val,maxAge,secure));
        }
        if(http_response!=null){
                Cookie c=new Cookie(key,val);
                c.setMaxAge(maxAge);
                c.setSecure(secure);
                http_response.addCookie(c);
        }
        return this;
    }
    public List<HTTP.Cookie> getCookies(){
        return cookies;
    }
}
