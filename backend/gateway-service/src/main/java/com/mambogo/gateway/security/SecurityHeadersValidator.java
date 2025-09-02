package com.mambogo.gateway.security;

import com.mambogo.gateway.config.SecurityHeadersProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;

/**
 * Comprehensive validator for security headers compliance and effectiveness.
 * 
 * This validator provides:
 * - Real-time security headers validation
 * - OWASP compliance checking
 * - Security policy validation
 * - Performance impact assessment
 * - Configuration validation
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Component
public class SecurityHeadersValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersValidator.class);
    
    private final SecurityHeadersProperties properties;
    
    // OWASP recommended security headers
    private static final Set<String> REQUIRED_SECURITY_HEADERS = Set.of(
        "X-Content-Type-Options",
        "X-Frame-Options",
        "X-XSS-Protection",
        "Referrer-Policy"
    );
    
    // Critical security headers for production
    private static final Set<String> CRITICAL_SECURITY_HEADERS = Set.of(
        "Strict-Transport-Security",
        "Content-Security-Policy",
        "Permissions-Policy"
    );

    public SecurityHeadersValidator(SecurityHeadersProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates security headers for a response.
     * 
     * @param exchange The server web exchange
     * @return Validation result with compliance status
     */
    public SecurityHeadersValidationResult validateResponseHeaders(ServerWebExchange exchange) {
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        SecurityHeadersValidationResult result = new SecurityHeadersValidationResult();
        result.setPath(path);
        result.setMethod(method);
        result.setTimestamp(new Date());
        
        try {
            // Validate required security headers
            validateRequiredHeaders(responseHeaders, result);
            
            // Validate critical security headers for production
            if (isProductionEnvironment()) {
                validateCriticalHeaders(responseHeaders, result);
            }
            
            // Validate header values for security compliance
            validateHeaderValues(responseHeaders, result);
            
            // Calculate overall compliance score
            calculateComplianceScore(result);
            
            // Log validation results
            logValidationResults(result);
            
        } catch (Exception e) {
            logger.error("Error validating security headers for {} {}: {}", 
                        method, path, e.getMessage(), e);
            result.addError("Validation error: " + e.getMessage());
            result.setValid(false);
        }
        
        return result;
    }

    /**
     * Validates required security headers are present.
     */
    private void validateRequiredHeaders(HttpHeaders headers, SecurityHeadersValidationResult result) {
        for (String requiredHeader : REQUIRED_SECURITY_HEADERS) {
            if (!headers.containsKey(requiredHeader)) {
                result.addError("Missing required security header: " + requiredHeader);
                result.setValid(false);
            } else {
                String value = headers.getFirst(requiredHeader);
                if (value == null || value.trim().isEmpty()) {
                    result.addError("Required security header has empty value: " + requiredHeader);
                    result.setValid(false);
                }
            }
        }
    }

    /**
     * Validates critical security headers for production environments.
     */
    private void validateCriticalHeaders(HttpHeaders headers, SecurityHeadersValidationResult result) {
        for (String criticalHeader : CRITICAL_SECURITY_HEADERS) {
            if (!headers.containsKey(criticalHeader)) {
                result.addWarning("Missing critical security header for production: " + criticalHeader);
            }
        }
    }

    /**
     * Validates header values for security compliance.
     */
    private void validateHeaderValues(HttpHeaders headers, SecurityHeadersValidationResult result) {
        // Validate X-Content-Type-Options
        String contentTypeOptions = headers.getFirst("X-Content-Type-Options");
        if (contentTypeOptions != null && !"nosniff".equals(contentTypeOptions)) {
            result.addWarning("X-Content-Type-Options should be 'nosniff', found: " + contentTypeOptions);
        }
        
        // Validate X-Frame-Options
        String frameOptions = headers.getFirst("X-Frame-Options");
        if (frameOptions != null) {
            Set<String> validFrameOptions = Set.of("DENY", "SAMEORIGIN");
            if (!validFrameOptions.contains(frameOptions) && !frameOptions.startsWith("ALLOW-FROM")) {
                result.addWarning("X-Frame-Options has potentially insecure value: " + frameOptions);
            }
        }
        
        // Validate X-XSS-Protection
        String xssProtection = headers.getFirst("X-XSS-Protection");
        if (xssProtection != null && !xssProtection.startsWith("1")) {
            result.addWarning("X-XSS-Protection should start with '1', found: " + xssProtection);
        }
        
        // Validate Referrer-Policy
        String referrerPolicy = headers.getFirst("Referrer-Policy");
        if (referrerPolicy != null) {
            Set<String> validReferrerPolicies = Set.of(
                "no-referrer", "no-referrer-when-downgrade", "origin", 
                "origin-when-cross-origin", "same-origin", "strict-origin", 
                "strict-origin-when-cross-origin", "unsafe-url"
            );
            if (!validReferrerPolicies.contains(referrerPolicy)) {
                result.addWarning("Referrer-Policy has potentially insecure value: " + referrerPolicy);
            }
        }
        
        // Validate Strict-Transport-Security for production
        if (isProductionEnvironment()) {
            String hsts = headers.getFirst("Strict-Transport-Security");
            if (hsts == null) {
                result.addWarning("Strict-Transport-Security is recommended for production environments");
            } else if (!hsts.contains("max-age=")) {
                result.addWarning("Strict-Transport-Security should include max-age directive");
            }
        }
    }

    /**
     * Calculates overall compliance score.
     */
    private void calculateComplianceScore(SecurityHeadersValidationResult result) {
        int totalChecks = result.getErrors().size() + result.getWarnings().size();
        if (totalChecks == 0) {
            result.setComplianceScore(100.0);
            return;
        }
        
        int errorWeight = 3; // Errors are 3x more important than warnings
        int totalWeightedScore = totalChecks * 100;
        int actualScore = (result.getErrors().size() * errorWeight * 100) + 
                         (result.getWarnings().size() * 100);
        
        result.setComplianceScore(Math.max(0.0, 100.0 - (double) actualScore / totalWeightedScore));
    }

    /**
     * Logs validation results for monitoring and debugging.
     */
    private void logValidationResults(SecurityHeadersValidationResult result) {
        if (!result.isValid()) {
            logger.warn("Security headers validation failed for {} {} - Score: {:.1f}%", 
                       result.getMethod(), result.getPath(), result.getComplianceScore());
            result.getErrors().forEach(error -> logger.warn("  Error: {}", error));
        }
        
        if (!result.getWarnings().isEmpty()) {
            logger.info("Security headers validation warnings for {} {} - Score: {:.1f}%", 
                       result.getMethod(), result.getPath(), result.getComplianceScore());
            result.getWarnings().forEach(warning -> logger.info("  Warning: {}", warning));
        }
        
        if (result.isValid() && result.getWarnings().isEmpty()) {
            logger.debug("Security headers validation passed for {} {} - Score: {:.1f}%", 
                        result.getMethod(), result.getPath(), result.getComplianceScore());
        }
    }

    /**
     * Checks if current environment is production.
     */
    private boolean isProductionEnvironment() {
        return properties.getEffectiveSecurityLevel() == SecurityHeadersProperties.SecurityLevel.PRODUCTION;
    }

    /**
     * Validates the current configuration for security compliance.
     */
    public void validateConfiguration() {
        List<String> configErrors = new ArrayList<>();
        
        // Validate security level configuration
        if (properties.getEffectiveSecurityLevel() == SecurityHeadersProperties.SecurityLevel.PRODUCTION) {
            if (properties.getXssProtection().isAllowUnsafeInline()) {
                configErrors.add("Production security level does not allow unsafe-inline");
            }
            if (!properties.getHttpsEnforcement().isForceHttps()) {
                configErrors.add("Production security level requires HTTPS enforcement");
            }
        }
        
        // Validate header value configurations
        if (properties.getMimeTypeProtection().isEnabled() && 
            !"nosniff".equals(properties.getMimeTypeProtection().getValue())) {
            configErrors.add("X-Content-Type-Options should be 'nosniff' for security");
        }
        
        if (properties.getClickjackingProtection().isEnabled() && 
            !"DENY".equals(properties.getClickjackingProtection().getXFrameOptions()) &&
            !"SAMEORIGIN".equals(properties.getClickjackingProtection().getXFrameOptions())) {
            configErrors.add("X-Frame-Options should be 'DENY' or 'SAMEORIGIN' for security");
        }
        
        if (!configErrors.isEmpty()) {
            String errorMessage = "Configuration validation failed: " + String.join("; ", configErrors);
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        logger.debug("Security headers configuration validation passed");
    }

    /**
     * Gets security headers compliance recommendations.
     */
    public List<String> getComplianceRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        recommendations.add("Ensure all required security headers are present in responses");
        recommendations.add("Use 'nosniff' for X-Content-Type-Options to prevent MIME type sniffing");
        recommendations.add("Use 'DENY' or 'SAMEORIGIN' for X-Frame-Options to prevent clickjacking");
        recommendations.add("Use '1; mode=block' for X-XSS-Protection (legacy browsers)");
        recommendations.add("Use 'strict-origin-when-cross-origin' for Referrer-Policy");
        
        if (isProductionEnvironment()) {
            recommendations.add("Enable Strict-Transport-Security with appropriate max-age");
            recommendations.add("Implement comprehensive Content-Security-Policy");
            recommendations.add("Use Permissions-Policy to restrict browser features");
        }
        
        return recommendations;
    }

    /**
     * Validation result class for security headers compliance.
     */
    public static class SecurityHeadersValidationResult {
        private String path;
        private String method;
        private Date timestamp;
        private boolean valid = true;
        private double complianceScore = 100.0;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> recommendations = new ArrayList<>();

        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getRecommendations() { return recommendations; }

        public void addError(String error) {
            errors.add(error);
            valid = false;
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addRecommendation(String recommendation) {
            recommendations.add(recommendation);
        }

        public String getSummary() {
            return String.format("Validation %s for %s %s (Score: %.1f%%, Errors: %d, Warnings: %d)",
                               valid ? "PASSED" : "FAILED", method, path, complianceScore, 
                               errors.size(), warnings.size());
        }
    }
}
