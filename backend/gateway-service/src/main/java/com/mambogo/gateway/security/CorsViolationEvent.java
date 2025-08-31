package com.mambogo.gateway.security;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.List;

/**
 * Application event for CORS security violations.
 * 
 * Published when security violations are detected during CORS processing.
 * Allows for decoupled handling of security incidents.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
public class CorsViolationEvent extends ApplicationEvent {
    
    private final String origin;
    private final String method;
    private final String path;
    private final String userAgent;
    private final String clientIP;
    private final List<SecurityViolation> violations;
    private final String severity;
    private final Instant timestamp;

    public CorsViolationEvent(Object source, String origin, String method, String path, 
                             String userAgent, String clientIP, List<SecurityViolation> violations, String severity) {
        super(source);
        this.origin = origin;
        this.method = method;
        this.path = path;
        this.userAgent = userAgent;
        this.clientIP = clientIP;
        this.violations = violations;
        this.severity = severity;
        this.timestamp = Instant.now();
    }

    public CorsViolationEvent(String origin, String method, String path, 
                             String userAgent, String clientIP, List<SecurityViolation> violations, String severity) {
        this(CorsViolationEvent.class, origin, method, path, userAgent, clientIP, violations, severity);
    }

    // Getters
    public String getOrigin() { return origin; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getUserAgent() { return userAgent; }
    public String getClientIP() { return clientIP; }
    public List<SecurityViolation> getViolations() { return violations; }
    public String getSeverity() { return severity; }
    public Instant getCorsTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("CorsViolationEvent{origin='%s', method='%s', path='%s', severity='%s', violations=%d, timestamp=%s}", 
                           origin, method, path, severity, violations.size(), timestamp);
    }
}
