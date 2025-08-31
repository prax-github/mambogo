package com.mambogo.gateway.csp;

import com.mambogo.gateway.config.CspPolicyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processes CSP violation events with intelligent analysis, pattern detection,
 * and automated response capabilities.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@Component
public class CspViolationProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CspViolationProcessor.class);

    private final CspPolicyProperties cspProperties;
    
    // Violation tracking for pattern detection
    private final ConcurrentHashMap<String, AtomicLong> violationsByOrigin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> violationsByDirective = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> violationsByBlockedUri = new ConcurrentHashMap<>();

    public CspViolationProcessor(CspPolicyProperties cspProperties) {
        this.cspProperties = cspProperties;
        logger.info("CSP Violation Processor initialized");
    }

    /**
     * Processes a CSP violation event with comprehensive analysis.
     */
    public void processViolation(CspViolationEvent violation) {
        if (violation == null) {
            logger.warn("Received null CSP violation event");
            return;
        }

        try {
            // Log the violation
            logViolation(violation);
            
            // Track violation patterns
            trackViolationPatterns(violation);
            
            // Analyze for security threats
            analyzeSecurityThreats(violation);
            
            // Check for violation rate limits
            checkViolationRateLimits(violation);
            
            logger.debug("Successfully processed CSP violation: {}", violation.getDescription());
            
        } catch (Exception e) {
            logger.error("Error processing CSP violation: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs the CSP violation with appropriate severity level.
     */
    private void logViolation(CspViolationEvent violation) {
        String logMessage = String.format(
            "CSP VIOLATION [%s] - Origin: %s, Document: %s, Directive: %s, Blocked: %s, IP: %s",
            violation.getSeverity(),
            violation.getOrigin(),
            violation.getDocumentUri(),
            violation.getViolatedDirective(),
            violation.getBlockedUri(),
            violation.getClientIP()
        );
        
        switch (violation.getSeverity()) {
            case CRITICAL:
            case HIGH:
                logger.error(logMessage);
                break;
            case MEDIUM:
                logger.warn(logMessage);
                break;
            case LOW:
            default:
                logger.info(logMessage);
                break;
        }
        
        // Additional details in debug mode
        if (logger.isDebugEnabled()) {
            logger.debug("CSP Violation Details: {}", violation);
        }
    }

    /**
     * Tracks violation patterns for analysis and alerting.
     */
    private void trackViolationPatterns(CspViolationEvent violation) {
        // Track violations by origin
        String origin = sanitizeOrigin(violation.getOrigin());
        violationsByOrigin.computeIfAbsent(origin, k -> new AtomicLong(0)).incrementAndGet();
        
        // Track violations by directive
        String directive = sanitizeDirective(violation.getViolatedDirective());
        violationsByDirective.computeIfAbsent(directive, k -> new AtomicLong(0)).incrementAndGet();
        
        // Track violations by blocked URI
        String blockedUri = sanitizeBlockedUri(violation.getBlockedUri());
        violationsByBlockedUri.computeIfAbsent(blockedUri, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("Updated violation patterns - Origin: {}, Directive: {}, BlockedUri: {}",
                    origin, directive, blockedUri);
    }

    /**
     * Analyzes violations for potential security threats.
     */
    private void analyzeSecurityThreats(CspViolationEvent violation) {
        if (violation.isSuspicious()) {
            logger.warn("SUSPICIOUS CSP VIOLATION DETECTED - {}", violation.getDescription());
            
            // Additional analysis for suspicious violations
            analyzeSuspiciousViolation(violation);
        }
        
        // Check for script injection attempts
        if (isScriptInjectionAttempt(violation)) {
            logger.error("POTENTIAL SCRIPT INJECTION DETECTED - {}", violation.getDescription());
        }
        
        // Check for data exfiltration attempts
        if (isDataExfiltrationAttempt(violation)) {
            logger.error("POTENTIAL DATA EXFILTRATION DETECTED - {}", violation.getDescription());
        }
    }

    /**
     * Performs detailed analysis of suspicious violations.
     */
    private void analyzeSuspiciousViolation(CspViolationEvent violation) {
        String blockedUri = violation.getBlockedUri();
        if (blockedUri == null) return;
        
        // Check for common attack patterns
        if (blockedUri.contains("javascript:")) {
            logger.error("JavaScript URI violation - possible XSS attempt: {}", violation.getDescription());
        }
        
        if (blockedUri.contains("data:text/html")) {
            logger.error("Data URI HTML violation - possible XSS attempt: {}", violation.getDescription());
        }
        
        if (violation.getSourceFile() != null && violation.getSourceFile().contains("eval")) {
            logger.error("Dynamic code execution violation - possible code injection: {}", violation.getDescription());
        }
    }

    /**
     * Checks if a violation indicates a script injection attempt.
     */
    private boolean isScriptInjectionAttempt(CspViolationEvent violation) {
        String violatedDirective = violation.getViolatedDirective();
        String blockedUri = violation.getBlockedUri();
        
        if (violatedDirective == null || blockedUri == null) {
            return false;
        }
        
        return violatedDirective.toLowerCase().contains("script-src") &&
               (blockedUri.contains("javascript:") || 
                blockedUri.contains("data:") ||
                blockedUri.contains("eval(") ||
                blockedUri.contains("setTimeout(") ||
                blockedUri.contains("setInterval("));
    }

    /**
     * Checks if a violation indicates a data exfiltration attempt.
     */
    private boolean isDataExfiltrationAttempt(CspViolationEvent violation) {
        String violatedDirective = violation.getViolatedDirective();
        String blockedUri = violation.getBlockedUri();
        
        if (violatedDirective == null || blockedUri == null) {
            return false;
        }
        
        return violatedDirective.toLowerCase().contains("connect-src") &&
               (blockedUri.contains("evil") || 
                blockedUri.contains("malicious") ||
                blockedUri.contains("attacker") ||
                blockedUri.matches(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")); // IP addresses
    }

    /**
     * Checks if an origin has exceeded violation rate limits.
     */
    private void checkViolationRateLimits(CspViolationEvent violation) {
        String origin = sanitizeOrigin(violation.getOrigin());
        AtomicLong originViolations = violationsByOrigin.get(origin);
        
        if (originViolations != null) {
            long violationCount = originViolations.get();
            
            if (violationCount > cspProperties.getMaxViolationsPerOrigin()) {
                logger.error("VIOLATION RATE LIMIT EXCEEDED for origin: {} (count: {})",
                           origin, violationCount);
                
                // Could trigger additional security measures here
                // e.g., temporary blocking, alerting security team, etc.
            }
        }
    }

    /**
     * Gets violation statistics for monitoring and analysis.
     */
    public ViolationStatistics getViolationStatistics() {
        return new ViolationStatistics(
            violationsByOrigin.size(),
            violationsByDirective.size(),
            violationsByBlockedUri.size(),
            getTotalViolationCount(),
            getMostViolatedOrigin(),
            getMostViolatedDirective()
        );
    }

    /**
     * Resets all violation tracking (useful for testing or periodic cleanup).
     */
    public void resetViolationTracking() {
        violationsByOrigin.clear();
        violationsByDirective.clear();
        violationsByBlockedUri.clear();
        logger.info("CSP violation tracking reset");
    }

    // Utility methods
    private String sanitizeOrigin(String origin) {
        if (origin == null || origin.isEmpty()) {
            return "unknown";
        }
        return origin.length() > 100 ? origin.substring(0, 100) : origin;
    }

    private String sanitizeDirective(String directive) {
        if (directive == null || directive.isEmpty()) {
            return "unknown";
        }
        return directive.length() > 50 ? directive.substring(0, 50) : directive;
    }

    private String sanitizeBlockedUri(String blockedUri) {
        if (blockedUri == null || blockedUri.isEmpty()) {
            return "unknown";
        }
        return blockedUri.length() > 200 ? blockedUri.substring(0, 200) : blockedUri;
    }

    private long getTotalViolationCount() {
        return violationsByOrigin.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    private String getMostViolatedOrigin() {
        return violationsByOrigin.entrySet().stream()
                .max((e1, e2) -> Long.compare(e1.getValue().get(), e2.getValue().get()))
                .map(entry -> entry.getKey() + " (" + entry.getValue().get() + ")")
                .orElse("none");
    }

    private String getMostViolatedDirective() {
        return violationsByDirective.entrySet().stream()
                .max((e1, e2) -> Long.compare(e1.getValue().get(), e2.getValue().get()))
                .map(entry -> entry.getKey() + " (" + entry.getValue().get() + ")")
                .orElse("none");
    }

    /**
     * Statistics class for violation analysis.
     */
    public static class ViolationStatistics {
        private final int uniqueOrigins;
        private final int uniqueDirectives;
        private final int uniqueBlockedUris;
        private final long totalViolations;
        private final String mostViolatedOrigin;
        private final String mostViolatedDirective;

        public ViolationStatistics(int uniqueOrigins, int uniqueDirectives, int uniqueBlockedUris,
                                 long totalViolations, String mostViolatedOrigin, String mostViolatedDirective) {
            this.uniqueOrigins = uniqueOrigins;
            this.uniqueDirectives = uniqueDirectives;
            this.uniqueBlockedUris = uniqueBlockedUris;
            this.totalViolations = totalViolations;
            this.mostViolatedOrigin = mostViolatedOrigin;
            this.mostViolatedDirective = mostViolatedDirective;
        }

        // Getters
        public int getUniqueOrigins() { return uniqueOrigins; }
        public int getUniqueDirectives() { return uniqueDirectives; }
        public int getUniqueBlockedUris() { return uniqueBlockedUris; }
        public long getTotalViolations() { return totalViolations; }
        public String getMostViolatedOrigin() { return mostViolatedOrigin; }
        public String getMostViolatedDirective() { return mostViolatedDirective; }
    }
}
