package com.mambogo.gateway.security;

import java.time.Instant;

/**
 * Represents a CORS security violation.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
public class SecurityViolation {
    
    private final String type;
    private final String severity;
    private final String details;
    private final Instant timestamp;

    public SecurityViolation(String type, String severity, String details) {
        this.type = type;
        this.severity = severity;
        this.details = details;
        this.timestamp = Instant.now();
    }

    // Getters
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("SecurityViolation{type='%s', severity='%s', details='%s', timestamp=%s}", 
                           type, severity, details, timestamp);
    }
}
