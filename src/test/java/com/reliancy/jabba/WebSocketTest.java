/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.Session.Listener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.reliancy.jabba.decor.Routed;
import com.reliancy.jabba.decor.WebSocket;
import com.reliancy.jabba.servlet.JettyApp;

/**
 * Integration tests for WebSocket functionality.
 * Tests the new WebSocket architecture using @WebSocket + @Routed annotations
 * and WebSocketSession argument-based endpoints.
 */
public class WebSocketTest {
    
    /**
     * Test application with WebSocket endpoints using new API.
     */
    public static class TestWebSocketApp extends JettyApp {
        private int messageCount = 0;
        
        public TestWebSocketApp() {
            super();
        }
        
        @Override
        public void configure(Config conf) throws Exception {
            super.configure(conf);
            // Import methods from this class to the router
            Router router = getRouter();
            if(router != null) {
                router.importMethods(this);
                router.compile();
            }
        }
        
        // Echo endpoint - sends back what it receives
        @Routed(path="/ws/echo")
        @WebSocket
        public void echoEndpoint(com.reliancy.jabba.WebSocketSession session) {
            session.onText(msg -> {
                try {
                    session.sendText("Echo: " + msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        // Simple endpoint that counts messages
        @Routed(path="/ws/counter")
        @WebSocket
        public void counterEndpoint(com.reliancy.jabba.WebSocketSession session) {
            session.onText(msg -> {
                try {
                    messageCount++;
                    session.sendText("Message #" + messageCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        // Endpoint with immediate response on connect
        @Routed(path="/ws/session")
        @WebSocket
        public void sessionEndpoint(com.reliancy.jabba.WebSocketSession session) {
            session.onText(msg -> {
                try {
                    session.sendText("Connected: " + session.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        // HTTP endpoint for comparison
        @Routed(path="/test")
        public String testHttp() {
            return "HTTP works";
        }
        
        public int getMessageCount() {
            return messageCount;
        }
    }
    
    /**
     * Simple WebSocket client for testing (using Jetty 12 Session.Listener API).
     */
    public static class TestWebSocketClient implements Session.Listener.AutoDemanding {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<Session> connectFuture = new CompletableFuture<>();
        private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
        private Session session;
        
        @Override
        public void onWebSocketOpen(Session session) {
            this.session = session;
            connectFuture.complete(session);
        }
        
        @Override
        public void onWebSocketText(String message) {
            messages.add(message);
        }
        
        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            closeFuture.complete(null);
        }
        
        @Override
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
        
        public void send(String message) throws Exception {
            if (session != null && session.isOpen()) {
                // Jetty 12 API: Session.sendText() directly
                session.sendText(message, null);
            }
        }
        
        public String receiveMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messages.poll(timeout, unit);
        }
        
        public void close() {
            if (session != null) {
                session.close();
            }
        }
        
        public CompletableFuture<Session> getConnectFuture() {
            return connectFuture;
        }
        
        public CompletableFuture<Void> getCloseFuture() {
            return closeFuture;
        }
    }
    
    private TestWebSocketApp app;
    private WebSocketClient wsClient;
    private int testPort;
    private String baseWsUrl;
    
    @Before
    public void setUp() throws Exception {
        // Use a random port to avoid conflicts
        testPort = 18090 + (int)(Math.random() * 1000);
        baseWsUrl = "ws://localhost:" + testPort;
        
        // Start test app
        app = new TestWebSocketApp();
        ArgsConfig config = new ArgsConfig();
        Config.SERVER_PORT.set(config, testPort);
        config.load();
        app.begin(config);
        
        // Wait for server to start
        int attempts = 0;
        while(!app.isStarted() && attempts < 20){
            Thread.sleep(100);
            attempts++;
        }
        if(!app.isStarted()){
            throw new Exception("Server failed to start on port " + testPort);
        }
        Thread.sleep(200);
        
        // Create WebSocket client
        wsClient = new WebSocketClient();
        wsClient.start();
    }
    
    @After
    public void tearDown() throws Exception {
        if (wsClient != null) {
            try {
                wsClient.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        if(app != null){
            try {
                if(app.isStarted()){
                    app.end();
                    Thread.sleep(300);
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            app = null;
        }
    }
    
    @Test
    public void testWebSocketEchoEndpoint() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        
        // Connect to echo endpoint
        URI uri = new URI(baseWsUrl + "/ws/echo");
        wsClient.connect(client, uri);
        
        // Wait for connection
        Session session = client.getConnectFuture().get(5, TimeUnit.SECONDS);
        assertNotNull("Connection should be established", session);
        assertTrue("Session should be open", session.isOpen());
        
        // Send a message
        client.send("Hello WebSocket");
        
        // Receive echo response
        String response = client.receiveMessage(5, TimeUnit.SECONDS);
        assertNotNull("Should receive response", response);
        assertEquals("Should echo back message", "Echo: Hello WebSocket", response);
        
        // Send another message
        client.send("Test 123");
        response = client.receiveMessage(5, TimeUnit.SECONDS);
        assertEquals("Should echo second message", "Echo: Test 123", response);
        
        // Close connection
        client.close();
        client.getCloseFuture().get(5, TimeUnit.SECONDS);
    }
    
    @Test
    public void testWebSocketCounterEndpoint() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        
        // Connect to counter endpoint
        URI uri = new URI(baseWsUrl + "/ws/counter");
        wsClient.connect(client, uri);
        
        // Wait for connection
        Session session = client.getConnectFuture().get(5, TimeUnit.SECONDS);
        assertNotNull("Connection should be established", session);
        
        // Send multiple messages
        client.send("msg1");
        String response1 = client.receiveMessage(5, TimeUnit.SECONDS);
        assertTrue("First response should contain counter", response1.contains("Message #"));
        
        client.send("msg2");
        String response2 = client.receiveMessage(5, TimeUnit.SECONDS);
        assertTrue("Second response should contain counter", response2.contains("Message #"));
        
        // Counter should have incremented
        assertNotEquals("Responses should be different", response1, response2);
        
        // Close connection
        client.close();
        client.getCloseFuture().get(5, TimeUnit.SECONDS);
    }
    
    @Test
    public void testWebSocketSessionEndpoint() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        
        // Connect to session endpoint
        URI uri = new URI(baseWsUrl + "/ws/session");
        wsClient.connect(client, uri);
        
        // Wait for connection
        Session session = client.getConnectFuture().get(5, TimeUnit.SECONDS);
        assertNotNull("Connection should be established", session);
        
        // Send a message to trigger the response
        client.send("ping");
        
        // Should receive message with session ID
        String response = client.receiveMessage(5, TimeUnit.SECONDS);
        assertNotNull("Should receive connection message", response);
        assertTrue("Message should contain 'Connected'", response.startsWith("Connected:"));
        
        // Close connection
        client.close();
        client.getCloseFuture().get(5, TimeUnit.SECONDS);
    }
    
    @Test
    public void testMultipleWebSocketClients() throws Exception {
        TestWebSocketClient client1 = new TestWebSocketClient();
        TestWebSocketClient client2 = new TestWebSocketClient();
        
        // Connect both clients to echo endpoint
        URI uri = new URI(baseWsUrl + "/ws/echo");
        wsClient.connect(client1, uri);
        wsClient.connect(client2, uri);
        
        // Wait for connections
        Session session1 = client1.getConnectFuture().get(5, TimeUnit.SECONDS);
        Session session2 = client2.getConnectFuture().get(5, TimeUnit.SECONDS);
        
        assertNotNull("Client 1 should connect", session1);
        assertNotNull("Client 2 should connect", session2);
        assertTrue("Client 1 session should be open", session1.isOpen());
        assertTrue("Client 2 session should be open", session2.isOpen());
        
        // Send messages from both clients
        client1.send("From Client 1");
        client2.send("From Client 2");
        
        // Receive responses
        String response1 = client1.receiveMessage(5, TimeUnit.SECONDS);
        String response2 = client2.receiveMessage(5, TimeUnit.SECONDS);
        
        assertEquals("Client 1 should receive its echo", "Echo: From Client 1", response1);
        assertEquals("Client 2 should receive its echo", "Echo: From Client 2", response2);
        
        // Close connections
        client1.close();
        client2.close();
        client1.getCloseFuture().get(5, TimeUnit.SECONDS);
        client2.getCloseFuture().get(5, TimeUnit.SECONDS);
    }
    
    @Test
    public void testHttpStillWorksWithWebSocket() throws Exception {
        // Verify HTTP endpoints still work when WebSocket is enabled
        java.net.URL url = new java.net.URL("http://localhost:" + testPort + "/test");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        assertEquals("HTTP endpoint should work", 200, responseCode);
        
        java.io.BufferedReader in = new java.io.BufferedReader(
            new java.io.InputStreamReader(conn.getInputStream()));
        String response = in.readLine();
        in.close();
        
        assertEquals("HTTP response should be correct", "HTTP works", response);
    }
}

