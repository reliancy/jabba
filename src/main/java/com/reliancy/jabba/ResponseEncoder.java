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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reliancy.rec.JSONEncoder;
import com.reliancy.util.CodeException;
import com.reliancy.jabba.ui.Rendering;
import com.reliancy.jabba.ui.Template;


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
    private static final Logger logger = LoggerFactory.getLogger(ResponseEncoder.class);
    
    protected Logger log(){
        return logger;
    }

    public ResponseEncoder(Response r){
        this(r,StandardCharsets.UTF_8);
        //response=r;
        //locale=Locale.getDefault();
    }
    public ResponseEncoder(Response r,Charset chset){
        response=r;
        //locale=loc;
        charSet=chset;
    }
    public ResponseEncoder setCharSet(Charset set){
        charSet=set;
        return this;
    }
    public OutputStream getOutputStream() throws IOException{
        if(out!=null) return out;
        response.commit();
        out=response.getOutputStream();
        if(out==null){
            out=new ByteArrayOutputStream();
        }
        writer=new OutputStreamWriter(out,charSet);
        return out;
    }
    public Writer getWriter() throws IOException{
        if(writer!=null) return writer;
        response.commit();
        writer=response.getWriter();
        if(writer==null){
            writer=new StringWriter();
        }
        return writer;
    }
    public void flush() throws IOException{
        if(writer!=null) writer.flush();
        if(out!=null) out.flush();
    }
    public ResponseEncoder writeBytes(byte[] buf,int offset,int len) throws IOException{
        
        try{
            response.transitionTo(ResponseState.WRITING);
            getOutputStream().write(buf,offset, len);
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    public ResponseEncoder writeString(CharSequence str) throws IOException{
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            wr.append(str);
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    public ResponseEncoder writeStream(InputStream is) throws IOException{
        byte[] buf=new byte[2*4096];
        int bytesRead=-1;
        // Get output stream first (this will commit if still in CONFIGURING)
        OutputStream os=getOutputStream();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            while((bytesRead=is.read(buf))!=-1){
                os.write(buf,0,bytesRead);
            }
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    public ResponseEncoder writeln(CharSequence msg,Object ... args) throws IOException{
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            if(args.length>0){
                msg=MessageFormat.format(msg.toString(),args);
            }
            wr.append(msg).append("\n");
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    public ResponseEncoder writeIterator(Iterator<String> it) throws IOException{
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            while(it.hasNext()) wr.append(it.next());
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    public ResponseEncoder writeReader(Reader rd) throws IOException{
        char[] buffer = new char[2*4096];
        int n = 0;
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=this.getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            while (-1 != (n = rd.read(buffer))) {
                wr.write(buffer, 0, n);
            }
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
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
    

    /** When an error occurs we need properly render exception.
     * if html is accepted we try to render a valid response with n error within a template so it fits with the app.
     * for all others we set error status code.
     * for json,xml and plain we render into a message template for the rest we do nothing.
     * this method returns true if a response was generated. in overloaded methods
     * if false is returned we can generate response the status is set to 500 already.
     * @param ex exception state
     * @return this encoder for chaining
     * @throws IOException if writing the error response fails
     */
    public ResponseEncoder writeError(Throwable ex) throws IOException{
        log().error("error:",ex);
        Request req=response.getRequest();
        if(response.getStatus()==null) response.setStatus(Response.HTTP_INTERNAL_ERROR);
        String accepted_format=req!=null?req.getHeader("Accept"):null;
        boolean present=accepted_format!=null;
        if(present && (accepted_format.contains("/html") || accepted_format.contains("/xhtml"))){
            // we have html request
            response.setContentType(HTTP.MIME_HTML);
            Template t=Template.find("/templates/error.hbs");
            if(t==null){ // no template found
                if(ex instanceof IOException) throw ((IOException)ex);
                else throw new RuntimeException(ex);
            }
            Rendering.begin(t).with(ex).end(response);
            return this;
        } 
        // next we format a few common and supported messages
        if(present && accepted_format.contains("/json")){
            response.setContentType(HTTP.MIME_JSON);
            String template=getErrorFormat();
            if(template==null){
                template="'{'\n\t\"status\":\"error\",\n\t\"title\":\"{0}\",\n\t\"message\":\"{1}\"\n'}'\n";
            }
            StringBuilder title=new StringBuilder();
            StringBuilder detail=new StringBuilder();
            CodeException.fillUserMessage(ex, detail, title);
            String body=MessageFormat.format(template,JSONEncoder.escape(title),JSONEncoder.escape(detail));
            writeString(body);
            return this;
        }
        if(present && accepted_format.contains("/xml")){
            response.setContentType(HTTP.MIME_XML);
            String template=getErrorFormat();
            if(template==null){
                template="<response>\n\t<status>error</status>\n\t<title>{0}</title>\n\t<message>{1}</message>\n</response>\n";
            }
            StringBuilder title=new StringBuilder();
            StringBuilder detail=new StringBuilder();
            CodeException.fillUserMessage(ex, detail, title);
            String body=MessageFormat.format(template,title,detail);
            writeString(body);
            return this;
        }
        if(present && accepted_format.contains("text/plain")){
            response.setContentType(HTTP.MIME_PLAIN);
            String template=getErrorFormat();
            if(template==null){
                template="status=error\n\ntitle={0}\n\nmessage={1}\n\n";
            }
            StringBuilder title=new StringBuilder();
            StringBuilder detail=new StringBuilder();
            CodeException.fillUserMessage(ex, detail, title);
            String body=MessageFormat.format(template,title,detail);
            writeString(body);
            return this;
        }
        String body=ex.toString();
        String template=getErrorFormat();
        if(template!=null){
            StringBuilder title=new StringBuilder();
            StringBuilder detail=new StringBuilder();
            CodeException.fillUserMessage(ex, detail, title);
            body=MessageFormat.format(template,title,detail);
        }
        this.writeString(body);
        return this;
    }

    public ResponseEncoder writeObject(Object ret) throws IOException{
        if(ret==null) return this;
        try{
            log().debug("ResponseEncoder.writeObject(): ret={}, retType={}", ret, ret.getClass().getName());
            // Get writer first (this will commit if still in CONFIGURING)
            Writer wr=getWriter();
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            log().debug("ResponseEncoder.writeObject(): got writer={}", wr != null ? wr.getClass().getName() : "null");
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
            }else if(ret instanceof Throwable){
                writeError((Throwable)ret);
            }else{
                String str = ret.toString();
                log().debug("ResponseEncoder.writeObject(): writing string, length={}", str.length());
                wr.append(str);
                log().debug("ResponseEncoder.writeObject(): string written");
            }
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    //////   Interface implementations
    @Override
    public void close() throws IOException {
        try {
            // If we're still writing, mark as written before closing
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
            // Close the writer/stream
            if(writer != null) {
                writer.close();
            } else if(out != null) {
                out.close();
            }
        } catch(IOException e) {
            // Ensure state is correct even if close fails
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
            throw e;
        }
    }
    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq,0,csq.length());
    }
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=this.getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            wr.append(csq,start,end);
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
    @Override
    public Appendable append(char c) throws IOException {
        // Get writer first (this will commit if still in CONFIGURING)
        Writer wr=this.getWriter();
        try{
            // Now transition to WRITING (state should be COMMITTED at this point)
            response.transitionTo(ResponseState.WRITING);
            wr.append(c);
        }finally{
            if(response.getState() == ResponseState.WRITING) {
                response.transitionTo(ResponseState.WRITTEN);
            }
        }
        return this;
    }
}
