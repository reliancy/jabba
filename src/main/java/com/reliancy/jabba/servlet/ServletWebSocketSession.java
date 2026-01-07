/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import com.reliancy.jabba.WebSocketSession;
import com.reliancy.jabba.Session;

/**
 * Servlet-based implementation of WebSocketSession.
 * Wraps Jakarta WebSocket Session to provide WebSocket functionality.
 */
class ServletWebSocketSession extends WebSocketSession {
    
    /** The underlying Jakarta WebSocket session. */
    private jakarta.websocket.Session nativeSession;
    
    public ServletWebSocketSession(String route, String remoteAddress, Session appSession) {
        super(route, remoteAddress, appSession);
    }
    
    // ========== Native Session Accessor ==========
    
    /**
     * Get the underlying Jakarta WebSocket session.
     * @return the native session, or null if not yet set
     */
    public jakarta.websocket.Session getNativeSession() {
        return nativeSession;
    }
    
    /**
     * Set the underlying Jakarta WebSocket session.
     * Called after the upgrade completes to wire up the native session.
     * @param nativeSession the Jakarta WebSocket session
     */
    public void setNativeSession(jakarta.websocket.Session nativeSession) {
        this.nativeSession = nativeSession;
    }
    
    // ========== Abstract Method Implementations ==========
    
    /**
     * Send text message to this client.
     */
    @Override
    public void sendText(String message) throws IOException {
        if (nativeSession == null || !nativeSession.isOpen()) {
            throw new IOException("WebSocket session is not open");
        }
        nativeSession.getBasicRemote().sendText(message);
    }
    
    /**
     * Send binary data to this client.
     */
    @Override
    public void sendBinary(byte[] data) throws IOException {
        if (nativeSession == null || !nativeSession.isOpen()) {
            throw new IOException("WebSocket session is not open");
        }
        nativeSession.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
    }
    
    /**
     * Close this WebSocket connection.
     */
    @Override
    public void close() throws IOException {
        if (nativeSession != null && nativeSession.isOpen()) {
            nativeSession.close();
        }
    }
    
    /**
     * Close with status code and reason.
     */
    @Override
    public void close(int code, String reason) throws IOException {
        if (nativeSession != null && nativeSession.isOpen()) {
            CloseReason.CloseCode closeCode = CloseReason.CloseCodes.getCloseCode(code);
            nativeSession.close(new CloseReason(closeCode, reason));
        }
    }
    
    /**
     * Check if connection is open.
     */
    @Override
    public boolean isOpen() {
        return nativeSession != null && nativeSession.isOpen();
    }
    
    // ========== Jakarta WebSocket Event Bridge ==========
    // These methods would be called by the Jakarta WebSocket endpoint to dispatch events
    
    /**
     * Called by Jakarta WebSocket endpoint when text message received.
     * Bridges to our callback system.
     */
    void onNativeTextMessage(String message) {
        handleText(message);
    }
    
    /**
     * Called by Jakarta WebSocket endpoint when binary message received.
     * Bridges to our callback system.
     */
    void onNativeBinaryMessage(byte[] data) {
        handleBinary(data);
    }
    
    /**
     * Called by Jakarta WebSocket endpoint when error occurs.
     * Bridges to our callback system.
     */
    void onNativeError(Throwable error) {
        handleError(error);
    }
    
    /**
     * Called by Jakarta WebSocket endpoint when connection closes.
     * Bridges to our callback system.
     */
    void onNativeClose(int code, String reason) {
        handleClose(code, reason);
    }
    /**
     * Endpoint instance that bridges Jakarta WebSocket callbacks into ServletWebSocketSession.
     * Must be public for Jakarta WebSocket to instantiate it.
     */
    public static final class BridgingEndpoint extends Endpoint {
        private final ServletWebSocketSession wrapper;

        public BridgingEndpoint(ServletWebSocketSession wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void onOpen(jakarta.websocket.Session session, EndpointConfig config) {
            wrapper.setNativeSession(session);

            // Text messages - use explicit type registration for Jakarta WebSocket API
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    wrapper.onNativeTextMessage(message);
                }
            });

            // Binary messages - use explicit type registration for Jakarta WebSocket API
            session.addMessageHandler(ByteBuffer.class, new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer bb) {
                    byte[] data = new byte[bb.remaining()];
                    bb.get(data);
                    wrapper.onNativeBinaryMessage(data);
                }
            });
        }

        @Override
        public void onClose(jakarta.websocket.Session session, CloseReason closeReason) {
            wrapper.onNativeClose(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
        }

        @Override
        public void onError(jakarta.websocket.Session session, Throwable thr) {
            wrapper.onNativeError(thr);
        }
    }
    /** 
     * Creates a new websocket session and upgrades the HTTP response to a websocket.
     * 
     * TODO: Implementation needed:
     * 1. Get ServerContainer from ServletContext
     * 2. Create ServerEndpointConfig programmatically
     * 3. Call upgrade on the container
     * 4. Wire up Jakarta WebSocket events to our callbacks (handleText, handleBinary, etc.)
     * 
     * @param response the response to upgrade to a websocket
     * @param route the route to upgrade to a websocket
     * @param appSession the app session to attach to the websocket session
     * @return the new websocket session
     */
    public static ServletWebSocketSession create(ServletResponse response, String route, Session appSession) {
        ServletRequest request = (ServletRequest) response.getRequest();
        ServletWebSocketSession session = new ServletWebSocketSession(route, request.getRemoteAddress(), appSession);
        HttpServletRequest httpReq = request.getHttpServletRequest();
        HttpServletResponse httpResp = response.getHttpServletResponse();
        
        // TODO: Perform the actual WebSocket upgrade here
        // ServerContainer container = (ServerContainer) httpReq.getServletContext()
        //     .getAttribute(ServerContainer.class.getName());
        // ... configure endpoint and upgrade ...
        // 1) Get ServerContainer from ServletContext using standard Jakarta WebSocket attribute
        Object attr = httpReq.getServletContext()
            .getAttribute(ServerContainer.class.getName());

        if (!(attr instanceof ServerContainer serverContainer)) {
            throw new IllegalStateException(
                "No jakarta.websocket.server.ServerContainer found in ServletContext. " +
                "Did you initialize Jakarta WebSocket in Jetty? " +
                "Ensure JettyWebSocketServletContainerInitializer is configured in JettyApp."
            );
        }

        // 2) Create endpoint instance and ServerEndpointConfig that returns THIS instance
        BridgingEndpoint endpoint = new BridgingEndpoint(session);

        ServerEndpointConfig.Configurator configurator = new ServerEndpointConfig.Configurator() {
            @Override
            public <T> T getEndpointInstance(Class<T> endpointClass) {
                return endpointClass.cast(endpoint);
            }
        };

        ServerEndpointConfig sec = ServerEndpointConfig.Builder
            .create(BridgingEndpoint.class, route) // path is required by the builder
            .configurator(configurator)
            .build();

        // 3) Upgrade (this performs the handshake + switches protocols). :contentReference[oaicite:4]{index=4}
        // Path params: pass empty unless you need them.
        Map<String, String> pathParams = Collections.emptyMap();
        try {
            serverContainer.upgradeHttpToWebSocket(httpReq, httpResp, sec, pathParams);
        } catch (IOException | DeploymentException e) {
            // Make sure your response is sane if upgrade fails (often the container already wrote).
            throw new RuntimeException("WebSocket upgrade failed", e);
        }

        return session;
    }
}