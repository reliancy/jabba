/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;

import com.reliancy.rec.JSONEncoder;
import com.reliancy.util.CodeException;

/**
 * This class will replace the Java writer.
 * It will have chainable calls. It will inherit lower level calls
 * and then extend with higher level. For example write, writeln but then writeJson etc.
 * implements closeable,autocloseable,appendable.
 * we do not close
 */
public class ResponseEncoder implements Appendable,Closeable{
    protected final Response response;
    //protected final Locale locale;
    protected Writer writer;
    protected OutputStream out;
    protected Charset charSet;
    protected String errorFmt;

    public ResponseEncoder(Response r){
        this(r,StandardCharsets.UTF_8);
        //response=r;
        //locale=Locale.getDefault();
    }
    public ResponseEncoder(Response r,Charset chset){
        response=r;
        //locale=loc;
        charSet=StandardCharsets.UTF_8;
    }
    public ResponseEncoder setCharSet(Charset set){
        charSet=set;
        return this;
    }
    public OutputStream getOutputStream() throws IOException{
        if(out!=null) return out;
        if(response.getStatus()==null) response.setStatus(Response.HTTP_OK);
        if(response.getContentType()==null) response.setContentType("application/octet-stream");
        if(response.http_response!=null){
            out=response.http_response.getOutputStream();
        }else if(response.byte_response!=null){
            out=response.byte_response;
        }else{
            out=new ByteArrayOutputStream();
        }
        writer=new OutputStreamWriter(out,charSet);
        return out;
    }
    public Writer getWriter() throws IOException{
        if(writer!=null) return writer;
        if(response.getStatus()==null) response.setStatus(Response.HTTP_OK);
        if(response.getContentType()==null) response.setContentType("text/plain;charset=utf-8");
        if(response.http_response!=null){
            writer=response.http_response.getWriter();
        }else if(response.char_response!=null){
            writer=response.char_response;
        }else if(response.byte_response!=null){
            out=response.byte_response;
            writer=new OutputStreamWriter(out,charSet);
        }else{
            writer=new StringWriter();
        }
        return writer;
    }
    public ResponseEncoder writeBytes(byte[] buf,int offset,int len) throws IOException{
        getOutputStream().write(buf,offset, len);
        return this;
    }
    public ResponseEncoder writeString(CharSequence str) throws IOException{
        getWriter().append(str);
        return this;
    }
    public ResponseEncoder writeStream(InputStream is) throws IOException{
        byte[] buf=new byte[2*4096];
        int bytesRead=-1;
        while((bytesRead=is.read(buf))!=-1){
            writeBytes(buf,0,bytesRead);
        }
        return this;
    }
    public ResponseEncoder writeln(CharSequence msg,Object ... args) throws IOException{
        if(args.length==0){
            getWriter().append(msg).append("\n");
        }else{
            String str=MessageFormat.format(msg.toString(),args);
            getWriter().append(str).append("\n");
        }
        return this;
    }
    public ResponseEncoder writeIterator(Iterator<String> it) throws IOException{
        Writer wr=getWriter();
        while(it.hasNext()) wr.append(it.next());
        return this;
    }
    public ResponseEncoder writeReader(Reader rd) throws IOException{
        char[] buffer = new char[2*4096];
        int n = 0;
        Writer wr=this.getWriter();
        while (-1 != (n = rd.read(buffer))) {
            wr.write(buffer, 0, n);
        }
        return this;
    }
    public ResponseEncoder setErrorFormat(String fmt){
        errorFmt=fmt;
        return this;
    }
    public String getErrorFormat(){
        return this.errorFmt;
    }
    public ResponseEncoder writeError(Throwable ex) throws IOException{
        if(errorFmt==null){
            this.writeString(ex.toString());
        }else{
            StringBuilder title=new StringBuilder();
            StringBuilder detail=new StringBuilder();
            CodeException.fillUserMessage(ex, detail, title);
            String body=MessageFormat.format(
                errorFmt,
                JSONEncoder.escape(title),
                JSONEncoder.escape(detail));
            writeString(body);
        }
        return this;
    }
    public ResponseEncoder writeObject(Object ret) throws IOException{
        if(ret==null) return this;
        Writer wr=getWriter();
        if(ret instanceof Iterator){
            Iterator<?> it=(Iterator<?>)ret;
            while(it.hasNext()){
                Object obj=it.next();
                writeObject(obj);
            }
        }else if(ret instanceof Collection){
            Collection<?> cret=(Collection<?>) ret;
            for(Object o:cret) writeObject(o);
        }else if(ret instanceof Reader){
            writeReader((Reader)ret);
        }else if(ret instanceof byte[]){
            byte[] bret=(byte[])ret;
            writeBytes(bret,0,bret.length);
        }else{
            wr.append(ret.toString());
        }
        //wr.append("\n");
        return this;
    }
    //////   Interface implementations
    @Override
    public void close() throws IOException {
        getWriter().close();
    }
    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq,0,csq.length());
    }
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        this.getWriter().append(csq,start,end);
        return this;
    }
    @Override
    public Appendable append(char c) throws IOException {
        this.getWriter().append(c);
        return this;
    }
}
