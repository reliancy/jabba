/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import com.reliancy.util.Path;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** SQL particular implementation of a terminal.
 * It will use a connection pool under it to take care of connection re-use.
 * 
 */
public class SQLTerminal implements Terminal{
    HikariConfig config = new HikariConfig();
    HikariDataSource ds;
    Path url;
    String quoteLeft="\"";  // quotes could be subject to sql flavour
    String quoteRight="\"";

    public SQLTerminal(String url){
        this.url=new Path(url);
        String proto=this.url.getProtocol();
        if(!proto.startsWith("jdbc:")) proto="jdbc:"+proto;
        String u=proto+"://"+this.url.getHost()+":"+this.url.getPort()+"/"+this.url.getDatabase();
        config.setJdbcUrl(u);
        config.setUsername(this.url.getUserid());
        config.setPassword(this.url.getPassword());
        //config.setAutoCommit(false); -- do this in batch cases only
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        ds = new HikariDataSource( config );
    }
    public Connection getConnection() throws SQLException{
        return ds.getConnection();
    }
    @Override
    public Action execute(Action q) throws IOException{
       // System.out.println("Executing..."+q.getTrait());
        Action.Trait tr=q.getTrait();
        if(tr instanceof Action.Load){
            Entity ent=q.getEntity();
            SQLReader reader=new SQLReader(ent,this);
            try {
                reader.open(q);
                q.setItems(reader);
            } catch (SQLException e) {
                reader.close();
                throw new IOException(e);
            }
            //System.out.println("Executing...Done");
            return q;
        }else if(tr instanceof Action.Save){
            Entity ent=q.getEntity();
            try(SQLWriter writer=new SQLWriter(ent,this)) {
                writer.open();
                writer.flush(q.getItems());
                //System.out.println("Executing...Done");
                return q;
            }catch(SQLException e){
                throw new IOException(e);
            }
        }else if(tr instanceof Action.Delete){
            Entity ent=q.getEntity();
            try(SQLCleaner cleaner=new SQLCleaner(ent,this)) {
                cleaner.open();
                cleaner.flush(q.getItems());
                //System.out.println("Executing...Done");
                return q;
            }catch(SQLException e){
                throw new IOException(e);
            }
        }else{
            throw new UnsupportedOperationException("Trait not supported:"+tr);
        }
    }
    public String getProtocol() {
        return url.getProtocol();
    }
    public String getQuoteLeft(){
        return this.quoteLeft;
    }
    public String getQuoteRight(){
        return this.quoteRight;
    }
    final HashMap<Integer,Class<?>> sql2java=new HashMap<>();
    final HashMap<Class<?>,Integer> java2sql=new HashMap<>();
    public Map<Class<?>,Integer> getJava2SQL(){
        if(!java2sql.isEmpty()) return java2sql;
        String protocol=url.getProtocol();
        java2sql.put(java.math.BigDecimal.class,Types.DECIMAL);
        java2sql.put(java.math.BigInteger.class,Types.DECIMAL);
        java2sql.put(Boolean.class,protocol.contains(":oracle")?Types.INTEGER:Types.BOOLEAN);
        java2sql.put(Byte.class,Types.TINYINT);
        java2sql.put(Short.class,Types.SMALLINT);
        java2sql.put(Integer.class,Types.INTEGER);
        java2sql.put(Long.class,Types.BIGINT);
        java2sql.put(Float.class,Types.FLOAT);
        java2sql.put(Double.class,Types.DOUBLE);
        java2sql.put(byte[].class,Types.VARBINARY);
        java2sql.put(Blob.class,Types.BLOB);
        java2sql.put(char[].class,Types.VARCHAR);
        java2sql.put(String.class,Types.VARCHAR);
        java2sql.put(StringBuffer.class,Types.VARCHAR);
        java2sql.put(Clob.class,Types.CLOB);
        java2sql.put(java.sql.Date.class,Types.DATE);
        java2sql.put(java.sql.Time.class,Types.TIME);
        java2sql.put(java.sql.Timestamp.class,Types.TIMESTAMP);
        java2sql.put(Array.class,Types.ARRAY);
        return java2sql;
    }
    public Map<Integer,Class<?>> getSQL2Java(){
        if(!sql2java.isEmpty()) return sql2java;
        //String protocol=url.getProtocol();
        sql2java.put(Types.NUMERIC,java.math.BigDecimal.class);
        sql2java.put(Types.DECIMAL,java.math.BigDecimal.class);
        sql2java.put(Types.BIT,Boolean.class);
        sql2java.put(Types.BOOLEAN,Boolean.class);
        sql2java.put(Types.TINYINT,Byte.class);
        sql2java.put(Types.SMALLINT,Short.class);
        sql2java.put(Types.INTEGER,Integer.class);
        sql2java.put(Types.BIGINT,Long.class);
        sql2java.put(Types.REAL,Float.class);
        sql2java.put(Types.FLOAT,Float.class);
        sql2java.put(Types.DOUBLE,Double.class);
        sql2java.put(Types.BINARY,byte[].class);
        sql2java.put(Types.VARBINARY,byte[].class);
        sql2java.put(Types.LONGVARBINARY,byte[].class);
        sql2java.put(Types.CHAR,String.class);
        sql2java.put(Types.NCHAR,String.class);
        sql2java.put(Types.VARCHAR,String.class);
        sql2java.put(Types.NVARCHAR,String.class);
        sql2java.put(Types.LONGVARCHAR,String.class);
        sql2java.put(Types.LONGNVARCHAR,String.class);
        sql2java.put(Types.DATE,java.sql.Date.class);
        sql2java.put(Types.TIME,java.sql.Time.class);
        sql2java.put(Types.TIMESTAMP,java.sql.Timestamp.class);
        sql2java.put(Types.BLOB,byte[].class);
        sql2java.put(Types.CLOB,char[].class);
        sql2java.put(Types.ARRAY,java.sql.Array.class);
        sql2java.put(Types.JAVA_OBJECT,Object.class);
        return sql2java;
    }
	/**
	 * Returns back java class for given id and or name.
	 * The name is not used in default implementation.
	 * @param typeid sql type to map
     * @return Class matching sql typeid.
	 */
    public Class<?> getJavaType(int typeid) {
        Class<?> ret=getSQL2Java().get(typeid);
        return ret;
    }
	/**
	 * This method will correct cases when sqltype is varchar (12) but type name is date or similar.
	 * @param sqltype
	 * @param type_name
	 * @return tries to promote sqltype given type name to something more specific.
	 */
	public int getTypeId(int sqltype,String type_name){
		if(type_name==null) return sqltype;
		type_name=type_name.toLowerCase();
		if(sqltype==Types.VARCHAR || sqltype==Types.CHAR){
			if(type_name.equals("date")) sqltype=Types.DATE;
			if(type_name.equals("time")) sqltype=Types.TIME;
			if(type_name.equals("datetime")) sqltype=Types.TIMESTAMP;
		}
		return sqltype;
	}

	/**
	 * @param cls
	 * @param createParams
	 * @return SQL type given java class and create params
	 */
	public int getTypeId(Class<?> cls,String createParams){
        int ret=getJava2SQL().get(cls);
        return ret;
	}
	public String getTypeName(Class<?> cls,String createParams){
		int id=this.getTypeId(cls, createParams);
        String ret = JDBCType.valueOf(id).getName();
		if(ret==null) return null;
        String protocol=url.getProtocol();
		if(protocol.contains(":sqlserver")){
			if("boolean".equalsIgnoreCase(ret)) ret="BIT";
			if("timestamp".equalsIgnoreCase(ret)) ret="DATETIME";
			if("double".equalsIgnoreCase(ret)) ret="float";
			if("float".equalsIgnoreCase(ret)) ret="real";
		}
		if(protocol.contains(":postgre")){
			if("varbinary".equalsIgnoreCase(ret)) ret="bytea";
			if("double".equalsIgnoreCase(ret)) ret="double precision";
		}
		if("varchar".equalsIgnoreCase(ret) && (createParams!=null && !createParams.isEmpty())){
			long size=Long.parseLong(createParams);
			if(protocol.contains(":sqlserver")) ret=size>8000?ret.concat("(").concat("MAX").concat(")"):ret.concat("(").concat(String.valueOf(size)).concat(")");
			else if(protocol.contains(":oracle")) ret=size>2000?"CLOB":ret.concat("(").concat(String.valueOf(size)).concat(")");
			else if(protocol.contains(":mysql")) ret=size>Character.MAX_VALUE?"TEXT":ret.concat("(").concat(String.valueOf(size)).concat(")");
			else if(protocol.contains(":h2")) ret=size>Integer.MAX_VALUE?"CLOB":ret.concat("(").concat(String.valueOf(size)).concat(")");
			else if(protocol.contains(":postgre")) ret=size>Character.MAX_VALUE?"TEXT":ret.concat("(").concat(String.valueOf(size)).concat(")");
			else ret=(size>Character.MAX_VALUE)?"CLOB":ret.concat("(").concat(String.valueOf(size)).concat(")");
		}
		String args=null;
		if(ret.indexOf('(')==-1 && createParams!=null && !createParams.isEmpty()){
			if("decimal".equalsIgnoreCase(ret)) args=createParams;
			if("numeric".equalsIgnoreCase(ret)) args=createParams;
		}
		if(args!=null){
			ret=ret.concat("(").concat(args).concat(")");
		}
		return ret;
        
	}
}
