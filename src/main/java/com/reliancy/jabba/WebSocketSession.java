/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Abstract WebSocket session representing a single client connection.
 * Provides callback-based API for handling WebSocket events.
 * 
 * Each connection gets its own session instance with:
 * - AppSession from the upgrade request (user context)
 * - Callback handlers for text, binary, error, close events
 * - Static registry for broadcasting to multiple clients
 */
public abstract class WebSocketSession {
    // Static registry of all active WebSocket sessions
    private static final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    
    // Instance fields
    protected String id;              // Unique session ID: route + "/" + remoteAddress
    protected String route;           // WebSocket route path (e.g., "/ws/echo")
    protected String remoteAddress;   // Client address
    protected Session appSession;  // User session from HTTP upgrade request
    
    // Callback handlers (set by user code)
    protected Consumer<String> textHandler;
    protected Consumer<byte[]> binaryHandler;
    protected Consumer<Throwable> errorHandler;
    protected BiConsumer<Integer, String> closeHandler;
    
    /**
     * Constructor - automatically registers session in static registry
     */
    protected WebSocketSession(String route, String remoteAddress, Session appSession) {
        this.route = route;
        this.appSession = appSession;
        setRemoteAddress(remoteAddress); // Sets address and builds ID
        allSessions.put(this.id, this);
    }
    
    /**
     * Set remote address and rebuild session ID.
     * Allows updating from IP to resolved name later.
     */
    public void setRemoteAddress(String remoteAddress) {
        // Remove old ID from registry if exists
        if (this.id != null) {
            allSessions.remove(this.id);
        }
        
        this.remoteAddress = remoteAddress;
        this.id = route + "/" + remoteAddress;
        
        // Re-register with new ID
        allSessions.put(this.id, this);
    }
    
    // ========== Send Methods (abstract - implemented by servlet/native) ==========
    
    /**
     * Send text message to this client
     */
    public abstract void sendText(String message) throws IOException;
    
    /**
     * Send binary data to this client
     */
    public abstract void sendBinary(byte[] data) throws IOException;
    
    /**
     * Close this WebSocket connection
     */
    public abstract void close() throws IOException;
    
    /**
     * Close with status code and reason
     */
    public abstract void close(int code, String reason) throws IOException;
    
    /**
     * Check if connection is open
     */
    public abstract boolean isOpen();
    
    // ========== Callback Setters (used by application code) ==========
    
    /**
     * Set handler for incoming text messages
     */
    public void onText(Consumer<String> handler) {
        this.textHandler = handler;
    }
    
    /**
     * Set handler for incoming binary messages
     */
    public void onBinary(Consumer<byte[]> handler) {
        this.binaryHandler = handler;
    }
    
    /**
     * Set handler for errors
     */
    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }
    
    /**
     * Set handler for connection close
     * @param handler receives (closeCode, reason)
     */
    public void onClose(BiConsumer<Integer, String> handler) {
        this.closeHandler = handler;
    }
    
    // ========== Internal Callback Invocation (called by implementation) ==========
    
    /**
     * Internal: Dispatch text message to handler
     */
    protected void handleText(String message) {
        if (textHandler != null) {
            try {
                textHandler.accept(message);
            } catch (Exception e) {
                handleError(e);
            }
        }
    }
    
    /**
     * Internal: Dispatch binary message to handler
     */
    protected void handleBinary(byte[] data) {
        if (binaryHandler != null) {
            try {
                binaryHandler.accept(data);
            } catch (Exception e) {
                handleError(e);
            }
        }
    }
    
    /**
     * Internal: Dispatch error to handler
     */
    protected void handleError(Throwable error) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(error);
            } catch (Exception e) {
                // Log error in error handler?
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Internal: Dispatch close event to handler and cleanup
     */
    protected void handleClose(int code, String reason) {
        try {
            if (closeHandler != null) {
                closeHandler.accept(code, reason);
            }
        } finally {
            // Remove from registry
            allSessions.remove(this.id);
        }
    }
    
    // ========== Getters ==========
    
    public String getId() {
        return id;
    }
    
    public String getRoute() {
        return route;
    }
    
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    public Session getAppSession() {
        return appSession;
    }
    
    // ========== Static Registry Methods for Broadcasting ==========
    
    /**
     * Get all sessions for a specific route
     */
    public static Collection<WebSocketSession> getSessionsForRoute(String route) {
        return allSessions.values().stream()
            .filter(s -> s.route.equals(route))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all active sessions
     */
    public static Collection<WebSocketSession> getAllSessions() {
        return allSessions.values();
    }
    
    /**
     * Broadcast text message to all clients on a route
     */
    public static void broadcast(String route, String message) {
        getSessionsForRoute(route).forEach(session -> {
            try {
                session.sendText(message);
            } catch (IOException e) {
                session.handleError(e);
            }
        });
    }
    
    /**
     * Broadcast text message to all connected clients
     */
    public static void broadcastAll(String message) {
        getAllSessions().forEach(session -> {
            try {
                session.sendText(message);
            } catch (IOException e) {
                session.handleError(e);
            }
        });
    }
    
    /**
     * Get session by ID
     */
    public static WebSocketSession getSession(String id) {
        return allSessions.get(id);
    }
    
    /**
     * Get count of active sessions
     */
    public static int getSessionCount() {
        return allSessions.size();
    }
    
    /**
     * Get count of sessions for a route
     */
    public static int getSessionCount(String route) {
        return (int) allSessions.values().stream()
            .filter(s -> s.route.equals(route))
            .count();
    }
}

