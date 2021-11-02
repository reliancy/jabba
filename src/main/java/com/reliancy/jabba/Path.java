package com.reliancy.jabba;

/** uri path decoded into tokens. */
public class Path {
    String url;
    String schema;
    String host;
    String db;
    String query;
    String[] db_parts;
    public Path(String url) {
        this.url = url.trim();
        db=this.url;
        // extract schema
        int schi=db.indexOf("://");
        if(schi!=-1){
            schema=db.substring(0,schi);
            db=db.substring(schi+3);
        }
        //extract host
        schi=db.indexOf("/");
        if(schi>0){
            host=db.substring(0,schi);
            db=db.substring(schi);
        }
        // extract query
        schi=db.indexOf("?");
        if(schi>0){
            query=db.substring(schi+1);
            db=db.substring(0,schi);
        }
        db_parts=db.split("/");
    }
    public String toString(){
        return getURL();
    }
    public String getURL() {
        if(url==null){
            StringBuilder buf=new StringBuilder();
            if(schema!=null) buf.append(schema).append("://");
            if(host!=null) buf.append(host);
            String db=getDB();
            if(db!=null) buf.append(db);
            String q=getQuery();
            if(q!=null) buf.append("?").append(q);
            url=buf.toString();
        }
        return url;
    }
    public void setURL(String url) {
        this.url = url;
    }
    public String getSchema() {
        return schema;
    }
    public void setSchema(String schema) {
        this.schema = schema;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getDB() {
        if(db==null && db_parts!=null) db="/"+String.join("/",db_parts);
        return db;
    }
    public void setDB(String db) {
        this.db = db;
    }
    public String[] getDBParts() {
        return db_parts;
    }
    public void setDBParts(String...parts) {
        db_parts=parts;
        db=null;
    }
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }
    
}
