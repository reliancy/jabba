package com.reliancy.dbo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.reliancy.util.Path;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class SQLTerminal implements Terminal{
    HikariConfig config = new HikariConfig();
    HikariDataSource ds;
    Path url;
    public SQLTerminal(String url){
        this.url=new Path(url);
        String proto=this.url.getProtocol();
        if(!proto.startsWith("jdbc:")) proto="jdbc:"+proto;
        String u=proto+"://"+this.url.getHost()+":"+this.url.getPort()+"/"+this.url.getDatabase();
        config.setJdbcUrl(u);
        config.setUsername(this.url.getUserid());
        config.setPassword(this.url.getPassword());
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        //config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        ds = new HikariDataSource( config );
    }
    public Connection getConnection() throws SQLException{
        return ds.getConnection();
    }
    @Override
    public Action execute(Action q) throws IOException {
        return q;
    }
    
}
