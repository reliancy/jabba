/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.reliancy.jabba.decor.Routed;
import com.reliancy.jabba.servlet.JettyApp;

/**
 * Integration tests for JettyApp regular (non-async) functionality.
 */
public class JettyAppTest {
    
    public static class SimpleTestApp extends JettyApp implements AppModule {
        @Override
        public void configure(Config conf) throws Exception {
            super.configure(conf);
            // Set up router and import methods
            Router router = getRouter();
            if(router == null){
                router = new Router();
                setRouter(router);
            }
            router.importMethods(this);
            router.compile();
        }
        
        @Override
        public void publish(App app) {
            app.getRouter().importMethods(this);
        }
        
        @Routed(path="/test")
        public String test() {
            return "test response";
        }
        
        @Routed(path="/testPlain")
        public void testPlain(Request req, Response resp) throws java.io.IOException {
            resp.getEncoder().writeln("plain response");
        }
        
        @Routed(path="/testParam/{id:int}")
        public String testParam(int id) {
            return "param: " + id;
        }
        
        @Routed(path="/testQuery")
        public String testQuery(String name) {
            return "query: " + name;
        }
        
        @Routed(path="/testNoArg")
        public String testNoArg() {
            return "no arg response";
        }
    }
    
    private SimpleTestApp app;
    private int testPort;
    private String baseUrl;
    
    @Before
    public void setUp() throws Exception {
        // Use a random port to avoid conflicts
        testPort = 18090 + (int)(Math.random() * 1000);
        baseUrl = "http://localhost:" + testPort;
        
        app = new SimpleTestApp();
        ArgsConfig config = new ArgsConfig();
        Config.SERVER_PORT.set(config, testPort);
        config.load();
        app.begin(config);
        
        // Wait for server to be started
        int attempts = 0;
        while(!app.isStarted() && attempts < 20){
            Thread.sleep(100);
            attempts++;
        }
        if(!app.isStarted()){
            throw new Exception("Server failed to start on port " + testPort);
        }
        // Give server a moment to be ready
        Thread.sleep(200);
    }
    
    @After
    public void tearDown() throws Exception {
        if(app != null){
            try {
                if(app.isStarted()){
                    app.end();
                    // Give server a moment to stop
                    Thread.sleep(300);
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            app = null;
        }
    }
    
    /**
     * Helper method to make HTTP GET request
     */
    private String httpGet(String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while((line = in.readLine()) != null){
                response.append(line);
            }
            in.close();
            return response.toString();
        }else{
            // Read error stream for more info
            String errorMsg = "HTTP request failed with code: " + responseCode;
            try {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String errLine;
                while((errLine = err.readLine()) != null){
                    errorMsg += "\n" + errLine;
                }
                err.close();
            } catch (Exception e) {
                // Ignore
            }
            throw new Exception(errorMsg);
        }
    }
    
    @Test
    public void testSimpleStringReturn() throws Exception {
        String result = httpGet("/test");
        assertEquals("Simple string return should work", "test response", result);
    }
    
    @Test
    public void testPlainRequestResponse() throws Exception {
        String result = httpGet("/testPlain");
        assertTrue("Plain request/response should work", result.contains("plain response"));
    }
    
    @Test
    public void testPathParameter() throws Exception {
        String result = httpGet("/testParam/42");
        assertEquals("Path parameter should work", "param: 42", result);
    }
    
    @Test
    public void testQueryParameter() throws Exception {
        String result = httpGet("/testQuery?name=testvalue");
        assertEquals("Query parameter should work", "query: testvalue", result);
    }
    
    @Test
    public void testNoArgMethod() throws Exception {
        String result = httpGet("/testNoArg");
        assertEquals("No-arg method should work", "no arg response", result);
    }
}

