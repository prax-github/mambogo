package com.mambogo.product.config;

/**
 * Constants for error response field names and codes to avoid hardcoded strings
 */
public final class ErrorResponseConstants {
    
    // Error response field names
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String PATH = "path";
    public static final String SERVICE = "service";
    
    // Validation error codes
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String SECURITY_VALIDATION_ERROR = "SECURITY_VALIDATION_ERROR";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String INVALID_INPUT_FORMAT = "INVALID_INPUT_FORMAT";
    public static final String REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING";
    public static final String FIELD_SIZE_VIOLATION = "FIELD_SIZE_VIOLATION";
    public static final String INVALID_FIELD_VALUE = "INVALID_FIELD_VALUE";
    
    // Security error codes
    public static final String XSS_DETECTED = "XSS_DETECTED";
    public static final String SQL_INJECTION_DETECTED = "SQL_INJECTION_DETECTED";
    public static final String PATH_TRAVERSAL_DETECTED = "PATH_TRAVERSAL_DETECTED";
    public static final String COMMAND_INJECTION_DETECTED = "COMMAND_INJECTION_DETECTED";
    public static final String SUSPICIOUS_INPUT_DETECTED = "SUSPICIOUS_INPUT_DETECTED";
    
    // Business rule error codes
    public static final String PRODUCT_NAME_INVALID = "PRODUCT_NAME_INVALID";
    public static final String PRODUCT_PRICE_INVALID = "PRODUCT_PRICE_INVALID";
    public static final String PRODUCT_CATEGORY_INVALID = "PRODUCT_CATEGORY_INVALID";
    public static final String PRODUCT_SKU_INVALID = "PRODUCT_SKU_INVALID";
    public static final String PRODUCT_DESCRIPTION_INVALID = "PRODUCT_DESCRIPTION_INVALID";
    
    // HTTP Status codes
    public static final class HttpStatus {
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int BAD_REQUEST = 400;
        public static final int UNPROCESSABLE_ENTITY = 422;
        
        private HttpStatus() {
            // Utility class
        }
    }
    
    private ErrorResponseConstants() {
        // Utility class - prevent instantiation
    }
}
