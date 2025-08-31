package com.mambogo.gateway.csp;

import java.time.Instant;

/**
 * Represents a Content Security Policy (CSP) violation event.
 * Contains all information reported by the browser when a CSP policy is violated.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
public class CspViolationEvent {

    private final String documentUri;
    private final String referrer;
    private final String violatedDirective;
    private final String effectiveDirective;
    private final String originalPolicy;
    private final String blockedUri;
    private final String sourceFile;
    private final Integer lineNumber;
    private final Integer columnNumber;
    private final String clientIP;
    private final String userAgent;
    private final String origin;
    private final Instant timestamp;

    public CspViolationEvent(String documentUri, String referrer, String violatedDirective,
                           String effectiveDirective, String originalPolicy, String blockedUri,
                           String sourceFile, Integer lineNumber, Integer columnNumber,
                           String clientIP, String userAgent, String origin, Instant timestamp) {
        this.documentUri = documentUri;
        this.referrer = referrer;
        this.violatedDirective = violatedDirective;
        this.effectiveDirective = effectiveDirective;
        this.originalPolicy = originalPolicy;
        this.blockedUri = blockedUri;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.clientIP = clientIP;
        this.userAgent = userAgent;
        this.origin = origin;
        this.timestamp = timestamp;
    }

    // Getters
    public String getDocumentUri() { return documentUri; }
    public String getReferrer() { return referrer; }
    public String getViolatedDirective() { return violatedDirective; }
    public String getEffectiveDirective() { return effectiveDirective; }
    public String getOriginalPolicy() { return originalPolicy; }
    public String getBlockedUri() { return blockedUri; }
    public String getSourceFile() { return sourceFile; }
    public Integer getLineNumber() { return lineNumber; }
    public Integer getColumnNumber() { return columnNumber; }
    public String getClientIP() { return clientIP; }
    public String getUserAgent() { return userAgent; }
    public String getOrigin() { return origin; }
    public Instant getTimestamp() { return timestamp; }

    /**
     * Returns the severity level of this violation based on the violated directive.
     */
    public ViolationSeverity getSeverity() {
        if (violatedDirective == null) {
            return ViolationSeverity.LOW;
        }
        
        String directive = violatedDirective.toLowerCase();
        
        // High severity violations
        if (directive.contains("script-src") || directive.contains("object-src") || 
            directive.contains("unsafe-inline") || directive.contains("unsafe-eval")) {
            return ViolationSeverity.HIGH;
        }
        
        // Medium severity violations
        if (directive.contains("frame-src") || directive.contains("frame-ancestors") ||
            directive.contains("form-action") || directive.contains("base-uri")) {
            return ViolationSeverity.MEDIUM;
        }
        
        // Low severity violations (style, img, font, etc.)
        return ViolationSeverity.LOW;
    }

    /**
     * Returns true if this violation might indicate a security attack.
     */
    public boolean isSuspicious() {
        if (blockedUri == null) {
            return false;
        }
        
        String uri = blockedUri.toLowerCase();
        
        // Check for suspicious patterns
        return uri.contains("javascript:") ||
               uri.contains("data:text/html") ||
               uri.contains("vbscript:") ||
               uri.contains("onload=") ||
               uri.contains("onerror=") ||
               uri.contains("<script") ||
               (sourceFile != null && sourceFile.contains("eval"));
    }

    /**
     * Returns a human-readable description of the violation.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (violatedDirective != null) {
            desc.append("CSP violation: ").append(violatedDirective);
        } else {
            desc.append("CSP violation");
        }
        
        if (blockedUri != null && !blockedUri.isEmpty()) {
            desc.append(" blocked URI: ").append(blockedUri);
        }
        
        if (sourceFile != null && !sourceFile.isEmpty()) {
            desc.append(" in file: ").append(sourceFile);
            
            if (lineNumber != null) {
                desc.append(" at line ").append(lineNumber);
                
                if (columnNumber != null) {
                    desc.append(" column ").append(columnNumber);
                }
            }
        }
        
        return desc.toString();
    }

    @Override
    public String toString() {
        return "CspViolationEvent{" +
                "documentUri='" + documentUri + '\'' +
                ", violatedDirective='" + violatedDirective + '\'' +
                ", blockedUri='" + blockedUri + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", lineNumber=" + lineNumber +
                ", columnNumber=" + columnNumber +
                ", clientIP='" + clientIP + '\'' +
                ", origin='" + origin + '\'' +
                ", timestamp=" + timestamp +
                ", severity=" + getSeverity() +
                ", suspicious=" + isSuspicious() +
                '}';
    }

    /**
     * Enumeration of CSP violation severity levels.
     */
    public enum ViolationSeverity {
        LOW,
        MEDIUM, 
        HIGH,
        CRITICAL
    }
}
