package com.reliancy.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Resources {
    public static interface PathRewrite{
        public String rewritePath(String path,Object context);
    }
    public static URL findFirst(PathRewrite remap,String path,Object ... sp){
        for(Object base:sp){
            if(remap!=null) path=remap.rewritePath(path,base);
            if(base instanceof Class){
                URL ret=((Class<?>)base).getResource(path);
                return ret;
            }else if(base instanceof String){
                File ff=new File(base.toString(),path);
                if(ff.exists()){
                    try {
                        return ff.toURI().toURL();
                    } catch (MalformedURLException e) {
                        continue;
                    }
                }
            }else if(base instanceof File){
                File ff=new File((File)base,path);
                if(ff.exists()){
                    try {
                        return ff.toURI().toURL();
                    } catch (MalformedURLException e) {
                        continue;
                    }
                }
            }else if(base instanceof URL){
                try {
                    URL ret=new URL((URL)base,path);
                    String proto=ret.getProtocol();
                    if(proto.equals("http") || proto.equals("https")){
                        HttpURLConnection huc = (HttpURLConnection) ret.openConnection();
                        huc.setRequestMethod("HEAD");
                        int responseCode = huc.getResponseCode();
                        huc.disconnect();
                        if(responseCode==HttpURLConnection.HTTP_OK) return ret;
                    }
                    if(proto.equals("file")){
                        File f=new File(ret.getPath());
                        if(f.exists()) return ret;
                    }
                } catch (MalformedURLException e) {
                    continue;
                } catch (IOException e2) {
                    continue;
                }
            }
        }
        return null;
    }
    public static String toString(URL url) throws IOException{
        return toString(url,StandardCharsets.UTF_8);
    }
    public static String toString(URL url,Charset chs) throws IOException{
        try(InputStream is=url.openStream()){
            return readChars(is,chs).toString();
        }
    }
    public static byte[] toBytes(URL url) throws IOException{
        try(InputStream is=url.openStream()){
            return readBytes(is);
        }
    }
    public static long copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    public static long copy(Reader input, Writer output, char[] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

	/** Reads a stream in one pass and returns bytes.
	 * Uses internally Handy.copy and a 4K buffer.
	 */
	public static final byte[] readBytes(InputStream str) throws IOException{
		ByteArrayOutputStream bout=new ByteArrayOutputStream();
		Resources.copy(str, bout, new byte[4096]);
		return bout.toByteArray();
	}
	public static final CharSequence readChars(InputStream str) throws IOException{
		return readChars(str,StandardCharsets.UTF_8);
	}
	public static final CharSequence readChars(InputStream str,Charset chset) throws IOException{
			BufferedReader rdr=new BufferedReader(new InputStreamReader(str,chset));
			StringBuilder ret=new StringBuilder();
			for(String line=rdr.readLine();line!=null;line=rdr.readLine()){
				ret.append(line).append("\n");
			}
			return ret;
	}
	public static CharSequence readChars(Class<?> cls,String name){
		InputStream io=cls.getResourceAsStream(name);
		try{
			return readChars(io);
		}catch(Exception e){
			return null;
		}finally{
			if(io!=null) try{io.close();}catch(Exception e){}
		}
	}
	public static void writeChars(CharSequence seq,OutputStream out,Charset chset) throws IOException{
		OutputStreamWriter dout=new OutputStreamWriter(out,chset);
		dout.append(seq);
		dout.flush();
	}
	public static void writeChars(CharSequence seq,OutputStream out) throws IOException{
		writeChars(seq,out,StandardCharsets.UTF_8);
	}
	public static void writeBytes(int offset,int len,byte[] seq,OutputStream out) throws IOException{
		out.write(seq,offset, len);
	}

}
