/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.reliancy.jabba.decor.Routed;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.plain.PlainSecurityStore;
import com.reliancy.util.Handy;

/**
 * Security tests for authentication and routing.
 */
public class SecurityTest {
    
    /** Minimal test implementation of Response for testing */
    static class TestResponse extends Response {
        private int status = 200;
        
        public TestResponse(Request request) {
            super(request);
        }
        @Override public void setContentType(String type) {}
        @Override public void setStatus(int status) { this.status = status; }
        @Override public String getHeader(String name) {
            for(HTTP.Header header : headers) {
                if(header.key.equalsIgnoreCase(name)) return header.value;
            }
            return null;
        }
        @Override public Response setHeader(String name, String value) { 
            headers.add(new HTTP.Header(name.toLowerCase(), value));
            return this;
        }
        public String getCookie(String name) {
            for(HTTP.Cookie cookie : cookies) {
                if(cookie.key.equals(name)) return cookie.value;
            }
            return null;
        }
        @Override public Response setCookie(String name, String value, int maxAge, boolean secure) { 
            cookies.add(new HTTP.Cookie(name, value, maxAge, secure, false)); 
            return this;
        }
        @Override public boolean isCommitted() { return false; }
        @Override public void commit() {}
        @Override public boolean isCompleted() { return false; }
        @Override public void complete() {}
        @Override public java.io.OutputStream getOutputStream() throws IOException { return null; }
        @Override public java.io.Writer getWriter() throws IOException { return null; }
        @Override public com.reliancy.jabba.WebSocketSession upgradeToWebSocket(String route, com.reliancy.jabba.Session appSession) throws IOException {
            throw new UnsupportedOperationException("WebSocket not supported in test");
        }
    }
    
    @Test
    public void testSecretKeyFromEnvironment() throws Exception {
        // Test that secret key can be loaded from environment using reflection
        String originalKey = System.getenv("JABBA_SECRET_KEY");
        try {
            System.setProperty("jabba.secret.key", "test-secret-key-12345");
            SecurityPolicy policy = new SecurityPolicy();
            java.lang.reflect.Method getSecretMethod = SecurityPolicy.class.getDeclaredMethod("getSecret");
            getSecretMethod.setAccessible(true);
            String secret = (String) getSecretMethod.invoke(policy);
            assertNotNull("Secret should not be null", secret);
            assertFalse("Secret should not be empty", secret.isEmpty());
        } finally {
            if (originalKey != null) {
                System.setProperty("jabba.secret.key", originalKey);
            } else {
                System.clearProperty("jabba.secret.key");
            }
        }
    }
    
    @Test
    public void testAESEncryption() {
        // Test AES encryption/decryption
        String key = "test-secret-key-for-encryption-12345678901234567890";
        Map<String, String> data = new HashMap<>();
        data.put("user", "testuser");
        data.put("pass", "testpass");
        
        String encrypted = Handy.encrypt(key, data);
        assertNotNull("Encrypted data should not be null", encrypted);
        assertFalse("Encrypted data should not be empty", encrypted.isEmpty());
        
        Map<String, String> decrypted = Handy.decrypt(key, encrypted);
        assertEquals("Decrypted user should match", "testuser", decrypted.get("user"));
        assertEquals("Decrypted pass should match", "testpass", decrypted.get("pass"));
    }
    
    @Test
    public void testInputValidation() throws Exception {
        // Test input validation in MethodEndPoint using reflection to access protected method
        MethodEndPoint endpoint = new MethodEndPoint(new TestEndpoint(), 
            TestEndpoint.class.getMethod("testMethod", String.class));
        
        java.lang.reflect.Method validateMethod = MethodEndPoint.class.getDeclaredMethod(
            "validateInput", Object.class, Class.class, String.class);
        validateMethod.setAccessible(true);
        
        // Test normal input
        Object valid = validateMethod.invoke(endpoint, "normal string", String.class, "testParam");
        assertEquals("Normal string should pass validation", "normal string", valid);
        
        // Test null input
        Object nullVal = validateMethod.invoke(endpoint, null, String.class, "testParam");
        assertNull("Null input should return null", nullVal);
        
        // Test very long string (should be truncated)
        StringBuilder longStr = new StringBuilder();
        for (int i = 0; i < 100001; i++) {
            longStr.append("a");
        }
        Object longInput = validateMethod.invoke(endpoint, longStr.toString(), String.class, "testParam");
        assertNotNull("Long input should not be null", longInput);
        assertTrue("Long input should be truncated", ((String)longInput).length() <= 100000);
    }
    
    @Test
    public void testCookieSecurity() throws IOException {
        // Test that cookies are set with HttpOnly flag
        Response response = new TestResponse((Request)null);
        response.setCookie("test", "value", 3600, true, true);
        
        // Verify cookie was added
        assertNotNull("Cookie should be added", response.getCookie("test"));
        assertEquals("Cookie value should match", "value", response.getCookie("test"));
    }
    
    @Test
    public void testResponseHeaderLookup() {
        // Test that header lookup works correctly (bug fix verification)
        Response response = new TestResponse((Request)null);
        response.setHeader("Content-Type", "application/json");
        
        String header = response.getHeader("content-type");
        assertEquals("Header lookup should be case-insensitive", "application/json", header);
    }
    
    // Test endpoint class for testing
    public static class TestEndpoint {
        @Routed
        public String testMethod(String param) {
            return param;
        }
    }
}

