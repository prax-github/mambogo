package com.mambogo.product.exception;

/**
 * Exception thrown when security validation fails (e.g., suspicious input detected).
 * Used to handle potential security threats like XSS, SQL injection, etc.
 */
public class SecurityValidationException extends RuntimeException {

    private final String threatType;
    private final String suspiciousInput;

    public SecurityValidationException(String message) {
        super(message);
        this.threatType = null;
        this.suspiciousInput = null;
    }

    public SecurityValidationException(String message, String threatType) {
        super(message);
        this.threatType = threatType;
        this.suspiciousInput = null;
    }

    public SecurityValidationException(String message, String threatType, String suspiciousInput) {
        super(message);
        this.threatType = threatType;
        this.suspiciousInput = suspiciousInput;
    }

    public SecurityValidationException(String message, Throwable cause) {
        super(message, cause);
        this.threatType = null;
        this.suspiciousInput = null;
    }

    public String getThreatType() {
        return threatType;
    }

    public String getSuspiciousInput() {
        return suspiciousInput;
    }
}
