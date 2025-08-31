package com.mambogo.product.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Security audit logger for validation failures and security events.
 * Provides structured logging for compliance and monitoring.
 */
@Component
public class SecurityAuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    /**
     * Log validation failure events
     */
    public void logValidationFailure(String userId, String endpoint, String errorType, 
                                   String errorMessage, String clientIp) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventType", "VALIDATION_FAILURE");
        auditEvent.put("service", "product-service");
        auditEvent.put("userId", sanitize(userId));
        auditEvent.put("endpoint", sanitize(endpoint));
        auditEvent.put("errorType", errorType);
        auditEvent.put("errorMessage", sanitize(errorMessage));
        auditEvent.put("clientIp", sanitize(clientIp));
        auditEvent.put("timestamp", Instant.now().toString());
        
        auditLogger.info("Validation failure: {}", auditEvent);
    }

    /**
     * Log security threat detection
     */
    public void logSecurityThreat(String userId, String endpoint, String threatType, 
                                String suspiciousInput, String clientIp) {
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "SECURITY_THREAT");
        securityEvent.put("service", "product-service");
        securityEvent.put("userId", sanitize(userId));
        securityEvent.put("endpoint", sanitize(endpoint));
        securityEvent.put("threatType", threatType);
        securityEvent.put("suspiciousInput", sanitizeForLogging(suspiciousInput));
        securityEvent.put("clientIp", sanitize(clientIp));
        securityEvent.put("timestamp", Instant.now().toString());
        securityEvent.put("severity", "HIGH");
        
        securityLogger.warn("Security threat detected: {}", securityEvent);
    }

    /**
     * Log business rule violations
     */
    public void logBusinessRuleViolation(String userId, String endpoint, String ruleName, 
                                       String violation, String clientIp) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventType", "BUSINESS_RULE_VIOLATION");
        auditEvent.put("service", "product-service");
        auditEvent.put("userId", sanitize(userId));
        auditEvent.put("endpoint", sanitize(endpoint));
        auditEvent.put("ruleName", ruleName);
        auditEvent.put("violation", sanitize(violation));
        auditEvent.put("clientIp", sanitize(clientIp));
        auditEvent.put("timestamp", Instant.now().toString());
        
        auditLogger.info("Business rule violation: {}", auditEvent);
    }

    /**
     * Log successful validation events (for compliance)
     */
    public void logValidationSuccess(String userId, String endpoint, String operation, String clientIp) {
        if (auditLogger.isDebugEnabled()) {
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", "VALIDATION_SUCCESS");
            auditEvent.put("service", "product-service");
            auditEvent.put("userId", sanitize(userId));
            auditEvent.put("endpoint", sanitize(endpoint));
            auditEvent.put("operation", operation);
            auditEvent.put("clientIp", sanitize(clientIp));
            auditEvent.put("timestamp", Instant.now().toString());
            
            auditLogger.debug("Validation success: {}", auditEvent);
        }
    }

    /**
     * Log authentication/authorization failures
     */
    public void logAuthenticationFailure(String userId, String endpoint, String reason, String clientIp) {
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "AUTHENTICATION_FAILURE");
        securityEvent.put("service", "product-service");
        securityEvent.put("userId", sanitize(userId));
        securityEvent.put("endpoint", sanitize(endpoint));
        securityEvent.put("reason", reason);
        securityEvent.put("clientIp", sanitize(clientIp));
        securityEvent.put("timestamp", Instant.now().toString());
        securityEvent.put("severity", "MEDIUM");
        
        securityLogger.warn("Authentication failure: {}", securityEvent);
    }

    /**
     * Log rate limiting violations
     */
    public void logRateLimitViolation(String userId, String endpoint, String limitType, 
                                    int currentCount, int limit, String clientIp) {
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "RATE_LIMIT_VIOLATION");
        securityEvent.put("service", "product-service");
        securityEvent.put("userId", sanitize(userId));
        securityEvent.put("endpoint", sanitize(endpoint));
        securityEvent.put("limitType", limitType);
        securityEvent.put("currentCount", currentCount);
        securityEvent.put("limit", limit);
        securityEvent.put("clientIp", sanitize(clientIp));
        securityEvent.put("timestamp", Instant.now().toString());
        securityEvent.put("severity", "MEDIUM");
        
        securityLogger.warn("Rate limit violation: {}", securityEvent);
    }

    /**
     * Sanitize input for safe logging
     */
    private String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        
        // Remove potential log injection characters
        return input.replaceAll("[\\r\\n\\t]", "_")
                   .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    /**
     * Sanitize suspicious input for logging with length limits
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        
        String sanitized = sanitize(input);
        
        // Truncate very long values
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...[TRUNCATED]";
        }
        
        return sanitized;
    }
}
