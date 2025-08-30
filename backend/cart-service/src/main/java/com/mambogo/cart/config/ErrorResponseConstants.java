package com.mambogo.cart.config;

/**
 * Constants for error response field names to avoid hardcoded strings
 */
public final class ErrorResponseConstants {
    
    // Error response field names
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String PATH = "path";
    public static final String SERVICE = "service";
    
    // HTTP Status codes
    public static final class HttpStatus {
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        
        private HttpStatus() {
            // Utility class
        }
    }
    
    private ErrorResponseConstants() {
        // Utility class - prevent instantiation
    }
}
