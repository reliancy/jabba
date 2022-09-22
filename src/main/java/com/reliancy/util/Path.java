/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.util;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;


/** Path to a resource almost identical to a URL.
 * It might but should not hold any handles. It holds the address and
 * possibly takes care of looking up addresses.
 * The richest syntax is:
 *  <pre> {@code PROTOCOL://USER:PWD@MACHINE:PORT/DATABASE?key=val&... } </pre>
 * Properties are held in their own string and need to be decoded.
 *
 * We use forward slash for path delimitation of database portion. For the rest we preserve other slashes to allow windows domain\\user or server\\instance.
 * Special chars are [/@?,:;]
 * if any are found at the end of protocol we skip ://
 * if any are found at the beginning of properties we skip ?
 * if any are found at the beginning of database we skip /
 * 
 * In windows we have volume or drive letters which are postfixed by colon : then slashes. We treat the volume as part of database.
 * We store the database without first slash to allow specification of relative paths.
 * To render it as absolute set protocol or host to empty string instead of null. Then a slash will be prefixed and you will get absolute path.
 * @author amer
 */
public class Path {
	static final String SYMBOLS="/@?,:;";
    String connectstring;
    String protocol;		///< protocol guides the interpretation of the other elements in conn, string
    String userid;			 ///< authentication
    String password;		///< authorization
    String host;			///< machine or computer
    String port;			///< access to computer
    String database;		///< name of database or filename
    String properties;     ///< properties are what follows ? in a url

    public Path(String connect) {
        setConnectString(connect);
    }

	public Path(Path in) {
		connectstring=in.connectstring;
		protocol=in.protocol;
		userid=in.userid;
		password=in.password;
		host=in.host;
		port=in.port;
		database=in.database;
		properties=in.properties;
	}

    @Override
    public String toString() {
        return getConnectString();
    }
	/**
	 * Converts the path to a file.
	 * If absolute is true
	 * @param absolute if true forms absolute path else will return relative path
	 * @return 
	 */
	public File toFile(boolean absolute){
		String proto=getProtocol();
		String host=getHost();
		try{
			setProtocol(absolute?"":null);
			setHost(absolute?"":null);
			String path=toString();
			return new File(path);
		}finally{
			setProtocol(proto);
			setHost(host);
		}
	}
	public URL toURL() throws MalformedURLException{
		String path=toString();
		if(Handy.isBlank(getHost()) && path.contains("://") && !path.contains(":///")) path=path.replace("://",":///");
		return new URL(path);
	}
	public void clear(){
		connectstring=null;
		protocol=null;
		userid=null;
		password=null;
		host=null;
		port=null;
		database=null;
		properties=null;
	}

    public String getConnectString() {
        if(connectstring!=null) return connectstring;
		// assemble the connect string
		StringBuilder buf=new StringBuilder();
		//boolean absolute=false;
		if(!Handy.isBlank(protocol)){
			buf.append(protocol);
			if(SYMBOLS.indexOf(protocol.charAt(protocol.length()-1))<0) buf.append("://");
		}
		if(!Handy.isBlank(host)){
			if(userid!=null && password!=null){
				buf.append(userid).append(":").append(password).append("@");
			}
			buf.append(host);
			if(port!=null) buf.append(":").append(port);
		}
		if(!Handy.isBlank(database)){
			if(buf.length()>0 && SYMBOLS.indexOf(database.charAt(0))<0){
				// we got something in front so we need to use slash
				buf.append("/");
			}else if(protocol!=null || host!=null){
				boolean winvol=database.length()>2 && database.charAt(1)==':' && (database.charAt(2)=='/' || database.charAt(2)=='\\');
				// we got nothing in front but if host or protocol empty but not null we treat as absolute
				if(!winvol) buf.append("/");
			}
			buf.append(database);
		}
		if(properties!=null){
			if(SYMBOLS.indexOf(properties.charAt(0))<0) buf.append("?");
			buf.append(properties);
		}
		connectstring=buf.toString();
		return connectstring;
    }

    public void setConnectString(String connect) {
		clear();
        if (connect == null) {
            return;
        }
		this.connectstring=connect;
        // first get protocol - everything up to : which is not followed by a symbol (includes :// but also c:/
		int oldst=0;
		int st=0;
		for(int i=0;i<(connectstring.length()-1);i++){
			char curr=connectstring.charAt(i);
			if(curr==':'){
				oldst=st;
				st=i;
			}else if(SYMBOLS.indexOf(curr)!=-1){
				if(curr=='@') st=oldst; // this will back out one : if protocl search ended with @ indicating a server
				break;
			}
		}
		if(st==1){ st=0;} // this will supress single letter protocols i.e. c:/ ued in windows as part of database/file
		if(2==(st-oldst)) st=oldst;
		if(st>0){
			this.protocol=connectstring.substring(0,st);
			while(SYMBOLS.indexOf(connectstring.charAt(st))!=-1) st++; // advance over symbols
		}
        // next assume the rest is a file/database
        database = connectstring.substring(st);
        // now check for user id and password
        st = database.indexOf('@');
		boolean checkhost=st>=0;
		if(!Handy.isBlank(protocol)){
			checkhost=!protocol.contains(":file") && !protocol.contains(":mem") && !protocol.equals("file") && !protocol.equals("mem");;
		}
        if (st != -1) {
            userid = database.substring(0, st);
			if(userid.contains("%4")) try{userid=URLDecoder.decode(userid,"UTF-8");}catch(Exception e){}
            database = database.substring(st + 1);
            // now try to split user id into password if possible
            st = userid.indexOf(':');
            if (st != -1) {
                password = userid.substring(st + 1);
                userid = userid.substring(0, st);
            }
        }
        // ok next try to split up machine if possible (only for absolute urls)
        st = database.indexOf(':');
		if(st<0) st=database.indexOf('/');
        if (st != -1 && checkhost) {
			boolean portfollows=database.charAt(st)==':';
            host = database.substring(0, st);
            // now try to recover port
			if(portfollows){
				int st2 = database.indexOf(':',st+1);
				if(st2<0) st2 = database.indexOf('/',st+1);
				if (st2 != -1 && st2>(st+1)) { // we have a port
					port = database.substring(st + 1,st2);
					st=st2;
				}else{
					// no port we have : then / - which is used in windows to indicate volume and we treat as part of database
					st=-1;
					host="";
				}
			}
            database = database.substring(st + 1);
        }
		database=fixSlashes(database);
        // finally split the properties from database
        st = database.indexOf('?');
		if(st==-1) st=database.indexOf(';');
        if (st != -1) { // we have properties
            properties = database.substring(st);
            database = database.substring(0, st);
        }
    }

    /**
     * Absolute ResourcePath will have a protocol
     */
    public boolean isAbsolute() {
        return (protocol != null || host!=null);
    }
	/// will clear host and protocol using empty string thereby making database absolute path
	public Path setAbsolute(){
		setHost("");
		setProtocol("");
		return this;
	}
	
    public String getDatabase() {
        return database;
    }

    public Path setDatabase(String database) {
        this.database = database;
		connectstring=null;
		return this;
    }

    public String getHost() {
        return host;
    }

    public Path setHost(String host) {
        this.host = host;
		connectstring=null;
		return this;
    }

    public String getPassword() {
        return password;
    }

    public Path setPassword(String password) {
        this.password = password;
		connectstring=null;
		return this;
    }

    public String getPort() {
        return port;
    }

    public Path setPort(String port) {
        this.port = port;
		connectstring=null;
		return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public Path setProtocol(String protocol) {
        this.protocol = protocol;
		connectstring=null;
		return this;
    }

    public String getUserid() {
        return userid;
    }

    public Path setUserid(String userid) {
        this.userid = userid;
		connectstring=null;
		return this;
    }
    public String getProperties() {
        return properties;
    }

    public Path setProperties(String userid) {
        this.properties = userid;
		connectstring=null;
		return this;
    }


    public String getBase() {
        return Path.getBase(database);
    }

    public String getExtension() {
        return Path.getExtension(database);
    }

    public String getPathItem() {
        return Path.getPathItem(database);
    }

	/** Ensures that we use forward slashes and that single dot is not present mid or and the end.
	 * 
	 * @param path a unix or windows or uri path
	 * @return a path with forward slashes
	 */
    public static String fixSlashes(String path) {
		if(path==null || path.length()==0) return path;
        path=path.replace("\\", "/");
		path=path.replace("/./","/");
		while(true){
			if(path.endsWith("/")) path=path.substring(0,path.length()-1);
			else if(path.endsWith("/.")) path=path.substring(0,path.length()-2);
			else break;
		}
		return path;
    }

    /** returns database path given path and file.
     * We assume the path uses forward backslash for delimitation.
     */
    public static String getBase(String path) {
        int st1 = path.lastIndexOf('/');
        int st2 = path.lastIndexOf('\\');
		int st=st2>st1?st2:st1;
        if (st == -1) {
            return null;
        }
        return path.substring(0, st);
    }

    public static String getExtension(String path) {
        int st=Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\'));
		int st2 = path.lastIndexOf('.');
        if (st2 == -1 || (st>0 && st2<st)) {
            return null;
        }
        return path.substring(st2 + 1);
    }

    public static String getPathItem(String path) {
		path=path.replace('\\','/');
        int st11 = path.lastIndexOf('/');
		int st1=1+st11;
        int st2 = path.lastIndexOf('.');
        if (st2 <0) {
            st2 = path.length();
        }
        return path.substring(st1, st2);
    }
    /**
     * Assuming that url starts with base will return string beyond base in url.
     * @param base
     * @param url
     */
    public static String getRemainder(String base,String url){
        if(base.length()>=url.length()) return null;
        return url.substring(base.length());
    }
	/**
	 * unites two paths.
	 * @param base
	 * @param url
	 */
	public static String getUnion(String base,String url){
        if(base==null || base.isEmpty()){
			return url;
		}
		if(url==null || url.isEmpty()){
			return base;
		}
		//if(url.startsWith(base)) return url;
		if(Handy.indexOf(url,base,0)==0) return url;
		StringBuilder ret=new StringBuilder();
		ret.append(base);
		if(!base.endsWith("/") && !url.startsWith("/")) ret.append("/");
		if(base.endsWith("/") && url.startsWith("/")) ret.setLength(ret.length()-1);
		ret.append(url);
        return ret.toString();
    }
	/**
	 * method will split paths used in linux and windows. 
	 * in particular for windows it checks if a single letter precedes a colon in which case it considers it a volume
	 * and does not split there.
	 * @param _paths paths joined with colon or semi-colon
	 * @return array of paths
	 */
	public static String[] splitPaths(String _paths){
		String[] paths=_paths.replaceAll("(;|:|^)([a-zA-Z]):","$1$2##").split("[:;]");
		for (int i = 0; i < paths.length; i++) {
			String path=paths[i];
			path = path.replace("##",":");
			path=path.replace("/./","/");
			path=path.replace("//","/");
			path=path.replace("\\.\\","\\");
			path=path.replace("\\\\","\\");
			paths[i]=path;
		}
		return paths;
    }
	
    /**
     * Returns a list of key,value pairs in the order they occur in the string str.
     * @param str
     */
    public static String[] splitProperties(String str) {
		if(str.startsWith("?")) str=str.substring(1);
		if(str.startsWith(";")) str=str.substring(1);
		return str.split("&");
    }
    public static String[] splitKeyValue(String str) {
		String[] t=str.split("=");
		if(t==null || t.length==0) return null;
		t[0]=Handy.trim(t[0],"'\"");
		return t;
    }
    public static String[] split(String str) {
		return str.split("/");
    }
}
