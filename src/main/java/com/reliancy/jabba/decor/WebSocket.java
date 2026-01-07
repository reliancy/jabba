/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.decor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a WebSocket endpoint.
 * WebSocket endpoints handle bidirectional communication with clients.
 * Works in conjunction with @Routed annotation for path mapping.
 * 
 * Example usage:
 * <pre>
 * {@literal @}Routed(path="/ws/chat")
 * {@literal @}WebSocket
 * public void handleChat(WebSocketSession session) {
 *     session.onMessage(msg -> {
 *         session.send("Echo: " + msg);
 *     });
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocket {
    /**
     * Optional subprotocols supported by this endpoint.
     * Example: {"mqtt", "stomp"}
     */
    String[] subprotocols() default {};
    
    /**
     * Maximum message size in bytes (default 64KB).
     * Set to -1 for unlimited.
     */
    int maxMessageSize() default 65536;
    
    /**
     * Idle timeout in milliseconds (default 5 minutes).
     * Connection closes if no messages received within this time.
     */
    long idleTimeout() default 300000;
}

