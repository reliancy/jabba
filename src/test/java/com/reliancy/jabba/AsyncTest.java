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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.reliancy.jabba.decor.Async;
import com.reliancy.jabba.decor.Routed;
import com.reliancy.jabba.servlet.JettyApp;

/**
 * Test async endpoint support.
 */
public class AsyncTest {
    
    public static class TestApp extends JettyApp {
        @Override
        public void configure(Config conf) throws Exception {
            super.configure(conf);
            // Import routes from this app - router is set by super.configure()
            Router router = getRouter();
            if(router != null){
                router.importMethods(this);
                router.compile();
            } else {
                // Router not set yet, set it ourselves
                Router newRouter = new Router();
                newRouter.importMethods(this);
                newRouter.compile();
                setRouter(newRouter);
            }
        }
        @Routed(path="/async")
        public CompletableFuture<String> asyncEndpoint() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate long-running operation
                    Thread.sleep(100);
                    return "Async result";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }
        
        @Routed(path="/sync")
        public String syncEndpoint() {
            return "Sync result";
        }
        
        @Routed(path="/asyncWithParam")
        public CompletableFuture<String> asyncWithParam(int delay) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(delay);
                    return "Delayed: " + delay + "ms";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }
        
        @Routed(path="/asyncAnnotation")
        @Async
        public String asyncWithAnnotation(String input, int value) {
            // Regular method with @Async annotation - should be detected as async
            return "Processed: " + input + " (" + value + ")";
        }
    }
    
    @Test
    public void testAsyncEndpointDetection() throws Exception {
        TestApp app = new TestApp();
        
        // Test async endpoint directly
        java.lang.reflect.Method asyncMethod = TestApp.class.getMethod("asyncEndpoint");
        MethodEndPoint asyncEp = new MethodEndPoint(app, asyncMethod);
        assertTrue("Endpoint should be detected as async", asyncEp.isAsync());
        
        // Test sync endpoint directly
        java.lang.reflect.Method syncMethod = TestApp.class.getMethod("syncEndpoint");
        MethodEndPoint syncEp = new MethodEndPoint(app, syncMethod);
        assertFalse("Endpoint should be detected as sync", syncEp.isAsync());
        
        // Test async with params
        java.lang.reflect.Method asyncParamMethod = TestApp.class.getMethod("asyncWithParam", int.class);
        MethodEndPoint asyncParamEp = new MethodEndPoint(app, asyncParamMethod);
        assertTrue("Endpoint with params should be detected as async", asyncParamEp.isAsync());
    }
    
    @Test
    public void testCompletableFutureReturnType() throws Exception {
        TestApp app = new TestApp();
        java.lang.reflect.Method method = TestApp.class.getMethod("asyncEndpoint");
        MethodEndPoint endpoint = new MethodEndPoint(app, method);
        
        assertTrue("Should detect CompletableFuture return type", endpoint.isAsync());
    }
    
    @Test
    public void testAsyncAnnotation() throws Exception {
        TestApp app = new TestApp();
        
        // Test method with @Async annotation and regular args/return type
        java.lang.reflect.Method asyncAnnotMethod = TestApp.class.getMethod("asyncWithAnnotation", String.class, int.class);
        MethodEndPoint asyncAnnotEp = new MethodEndPoint(app, asyncAnnotMethod);
        
        // Should be detected as async because of @Async annotation
        assertTrue("Endpoint with @Async annotation should be detected as async", asyncAnnotEp.isAsync());
        
        // Verify it has regular return type (not CompletableFuture)
        assertFalse("Return type should not be CompletableFuture", 
                   CompletableFuture.class.isAssignableFrom(asyncAnnotEp.method.getReturnType()));
        
        // Verify it has regular parameters
        assertEquals("Should have 2 parameters", 2, asyncAnnotEp.method.getParameterCount());
    }
    
    private TestApp app;
    private int testPort;
    private String baseUrl;
    
    @Before
    public void setUp() throws Exception {
        // Use a random port to avoid conflicts
        testPort = 18090 + (int)(Math.random() * 1000);
        baseUrl = "http://localhost:" + testPort;
        
        app = new TestApp();
        ArgsConfig config = new ArgsConfig();
        Config.SERVER_PORT.set(config, testPort);
        config.load();
        app.begin(config);
        
        // Wait for server to be started (not necessarily running, which requires work() to be called)
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
                if(app.isRunning()){
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
        conn.setReadTimeout(15000); // Longer timeout for async operations
        
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
    public void testSyncEndpointIntegration() throws Exception {
        // Test synchronous endpoint first to verify basic connectivity
        String result = httpGet("/sync");
        assertEquals("Sync endpoint should return correct result", "Sync result", result);
    }
    
    @Test
    public void testAsyncEndpointIntegration() throws Exception {
        // Test CompletableFuture return type endpoint
        long startTime = System.currentTimeMillis();
        String result = httpGet("/async");
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("Async endpoint should return correct result", "Async result", result);
        // Should take at least 100ms (the sleep time in the endpoint)
        assertTrue("Async endpoint should take time", duration >= 90);
    }
    
    
    @Test
    public void testAsyncWithParamIntegration() throws Exception {
        // Test async endpoint with parameters
        long startTime = System.currentTimeMillis();
        String result = httpGet("/asyncWithParam?delay=50");
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Result should contain delay info", result.contains("Delayed: 50ms"));
        // Should take at least 50ms
        assertTrue("Async endpoint with delay should take time", duration >= 40);
    }
    
    @Test
    public void testAsyncAnnotationIntegration() throws Exception {
        // Test @Async annotation endpoint
        String result = httpGet("/asyncAnnotation?input=test&value=42");
        
        assertEquals("Async annotation endpoint should return correct result", 
                    "Processed: test (42)", result);
    }
    
    @Test
    public void testAsyncNonBlocking() throws Exception {
        // Test that async endpoints don't block the server
        // First verify the endpoint works with a single request
        String singleResult = httpGet("/async");
        assertEquals("Single async request should work", "Async result", singleResult);
        
        // Make multiple concurrent requests
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try { 
                return httpGet("/async"); 
            } catch (Exception e) { 
                e.printStackTrace();
                return "ERROR: " + e.getMessage(); 
            }
        });
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try { 
                return httpGet("/async"); 
            } catch (Exception e) { 
                e.printStackTrace();
                return "ERROR: " + e.getMessage(); 
            }
        });
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            try { 
                return httpGet("/async"); 
            } catch (Exception e) { 
                e.printStackTrace();
                return "ERROR: " + e.getMessage(); 
            }
        });
        
        // Wait for all to complete
        CompletableFuture.allOf(future1, future2, future3).join();
        
        // All should succeed
        String result1 = future1.get();
        String result2 = future2.get();
        String result3 = future3.get();
        
        assertEquals("First request should succeed", "Async result", result1);
        assertEquals("Second request should succeed", "Async result", result2);
        assertEquals("Third request should succeed", "Async result", result3);
    }
}

