package com.mambogo.gateway.sanitization;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manages sanitization policies for different endpoints and request types.
 * Provides endpoint-specific sanitization rules and policy enforcement.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Component
public class SanitizationPolicyManager {

    private static final Logger logger = LoggerFactory.getLogger(SanitizationPolicyManager.class);
    
    private final InputSanitizationProperties properties;
    private final PolicyFactory htmlPolicy;
    private final Map<String, SanitizationPolicy> policyCache = new ConcurrentHashMap<>();
    
    // Whitelist patterns for bypassing sanitization
    private static final Pattern WHITELIST_PATTERN = Pattern.compile(
        "^(/actuator/.*|/api/csp/.*|/api/health/.*)$"
    );

    public SanitizationPolicyManager(InputSanitizationProperties properties) {
        this.properties = properties;
        this.htmlPolicy = createHtmlPolicy();
        initializePolicies();
    }

    /**
     * Gets the appropriate sanitization policy for a given endpoint.
     */
    public SanitizationPolicy getPolicyForEndpoint(String path) {
        String endpoint = extractEndpointFromPath(path);
        
        // Check whitelist first
        if (isWhitelisted(path)) {
            return createBypassPolicy();
        }
        
        return policyCache.getOrDefault(endpoint, getDefaultPolicy());
    }

    /**
     * Sanitizes input according to the specified policy.
     */
    public String sanitizeInput(String input, SanitizationPolicy policy, InputType inputType) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        if (policy.isBypass()) {
            return input;
        }
        
        switch (inputType) {
            case REQUEST_BODY:
                return sanitizeRequestBody(input, policy);
            case QUERY_PARAMETER:
                return sanitizeQueryParameter(input, policy);
            case HEADER_VALUE:
                return sanitizeHeaderValue(input, policy);
            case PATH_PARAMETER:
                return sanitizePathParameter(input, policy);
            default:
                return sanitizeGeneral(input, policy);
        }
    }

    private String sanitizeRequestBody(String input, SanitizationPolicy policy) {
        if (!policy.isSanitizeRequestBody()) {
            return input;
        }
        
        String sanitized = input;
        
        // Remove null bytes and control characters
        sanitized = sanitized.replaceAll("\\x00", "");
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        if (policy.isStrictMode()) {
            // In strict mode, apply more aggressive sanitization
            sanitized = htmlPolicy.sanitize(sanitized);
            sanitized = removeScriptContent(sanitized);
            sanitized = sanitized.replaceAll("(?i)javascript:", "");
            sanitized = sanitized.replaceAll("(?i)vbscript:", "");
        }
        
        return sanitized.trim();
    }

    private String sanitizeQueryParameter(String input, SanitizationPolicy policy) {
        if (!policy.isSanitizeQueryParams()) {
            return input;
        }
        
        String sanitized = input;
        
        // Basic sanitization
        sanitized = sanitized.replaceAll("\\x00", "");
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        // Remove SQL injection patterns
        sanitized = sanitized.replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter)", "");
        
        // Remove script tags and javascript
        sanitized = sanitized.replaceAll("(?i)(<script|</script>|javascript:|vbscript:)", "");
        
        if (policy.isStrictMode()) {
            // Allow only alphanumeric, spaces, and basic punctuation
            sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s\\-_.,!?@#$%()\\[\\]]", "");
        }
        
        return sanitized.trim();
    }

    private String sanitizeHeaderValue(String input, SanitizationPolicy policy) {
        if (!policy.isSanitizeHeaders()) {
            return input;
        }
        
        String sanitized = input;
        
        // Remove CRLF injection patterns
        sanitized = sanitized.replaceAll("[\\r\\n]", "");
        
        // Remove null bytes
        sanitized = sanitized.replaceAll("\\x00", "");
        
        // Basic script removal
        sanitized = sanitized.replaceAll("(?i)(<script|javascript:|vbscript:)", "");
        
        return sanitized.trim();
    }

    private String sanitizePathParameter(String input, SanitizationPolicy policy) {
        String sanitized = input;
        
        // Remove path traversal patterns
        sanitized = sanitized.replaceAll("(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c)", "");
        
        // Remove null bytes
        sanitized = sanitized.replaceAll("\\x00", "");
        
        // URL decode if needed and re-sanitize
        if (sanitized.contains("%")) {
            try {
                sanitized = java.net.URLDecoder.decode(sanitized, "UTF-8");
                sanitized = sanitized.replaceAll("(\\.\\./|\\.\\.\\\\)", "");
            } catch (Exception e) {
                logger.warn("Failed to URL decode path parameter: {}", input);
            }
        }
        
        return sanitized;
    }

    private String sanitizeGeneral(String input, SanitizationPolicy policy) {
        String sanitized = input;
        
        // Remove null bytes and control characters
        sanitized = sanitized.replaceAll("\\x00", "");
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Basic HTML sanitization
        if (policy.isStrictMode()) {
            sanitized = htmlPolicy.sanitize(sanitized);
        }
        
        return sanitized.trim();
    }

    private String removeScriptContent(String input) {
        String sanitized = input;
        
        // Remove script tags and content
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        // Remove event handlers
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        
        // Remove javascript: and vbscript: protocols
        sanitized = sanitized.replaceAll("(?i)(javascript|vbscript):", "");
        
        return sanitized;
    }

    private void initializePolicies() {
        // Initialize endpoint-specific policies based on configuration
        for (Map.Entry<String, InputSanitizationProperties.EndpointPolicy> entry : properties.getEndpointPolicies().entrySet()) {
            String endpoint = entry.getKey();
            InputSanitizationProperties.EndpointPolicy config = entry.getValue();
            
            SanitizationPolicy policy = new SanitizationPolicy(
                config.getPolicyName(),
                config.isSanitizeRequestBody(),
                config.isSanitizeQueryParams(),
                config.isSanitizeHeaders(),
                config.isEnableStrictMode(),
                false // not bypass
            );
            
            policyCache.put(endpoint, policy);
            
            logger.info("Initialized sanitization policy '{}' for endpoint '{}': body={}, query={}, headers={}, strict={}", 
                policy.getName(), endpoint, policy.isSanitizeRequestBody(), 
                policy.isSanitizeQueryParams(), policy.isSanitizeHeaders(), policy.isStrictMode());
        }
    }

    private PolicyFactory createHtmlPolicy() {
        // Create a permissive HTML policy for content sanitization
        return Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.STYLES);
    }

    private String extractEndpointFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        
        // Extract the main service/endpoint from the path
        // /api/products -> products
        // /api/cart -> cart
        // /api/orders -> orders
        // /api/payments -> payments
        String[] segments = path.split("/");
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2];
        }
        
        return "unknown";
    }

    private boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        
        return properties.getWhitelistPatterns().stream()
            .anyMatch(pattern -> path.matches(pattern)) || 
               WHITELIST_PATTERN.matcher(path).matches();
    }

    private SanitizationPolicy createBypassPolicy() {
        return new SanitizationPolicy("bypass", false, false, false, false, true);
    }

    private SanitizationPolicy getDefaultPolicy() {
        return new SanitizationPolicy("default", true, true, true, false, false);
    }

    /**
     * Enumeration of input types for sanitization
     */
    public enum InputType {
        REQUEST_BODY,
        QUERY_PARAMETER,
        HEADER_VALUE,
        PATH_PARAMETER,
        GENERAL
    }

    /**
     * Sanitization policy configuration
     */
    public static class SanitizationPolicy {
        private final String name;
        private final boolean sanitizeRequestBody;
        private final boolean sanitizeQueryParams;
        private final boolean sanitizeHeaders;
        private final boolean strictMode;
        private final boolean bypass;

        public SanitizationPolicy(String name, boolean sanitizeRequestBody, 
                                boolean sanitizeQueryParams, boolean sanitizeHeaders, 
                                boolean strictMode, boolean bypass) {
            this.name = name;
            this.sanitizeRequestBody = sanitizeRequestBody;
            this.sanitizeQueryParams = sanitizeQueryParams;
            this.sanitizeHeaders = sanitizeHeaders;
            this.strictMode = strictMode;
            this.bypass = bypass;
        }

        public String getName() { return name; }
        public boolean isSanitizeRequestBody() { return sanitizeRequestBody; }
        public boolean isSanitizeQueryParams() { return sanitizeQueryParams; }
        public boolean isSanitizeHeaders() { return sanitizeHeaders; }
        public boolean isStrictMode() { return strictMode; }
        public boolean isBypass() { return bypass; }
    }
}
