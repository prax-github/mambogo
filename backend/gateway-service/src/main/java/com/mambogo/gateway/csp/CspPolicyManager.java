package com.mambogo.gateway.csp;

import com.mambogo.gateway.config.CspPolicyProperties;
import com.mambogo.gateway.config.CorsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages Content Security Policy (CSP) generation, validation, and dynamic policy updates.
 * Provides comprehensive CSP policy management with environment-specific configurations,
 * nonce generation, and performance optimization.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@Component
public class CspPolicyManager {

    private static final Logger logger = LoggerFactory.getLogger(CspPolicyManager.class);

    private final CspPolicyProperties cspProperties;
    private final CorsProperties corsProperties;
    private final SecureRandom secureRandom;
    private final String activeEnvironment;

    // Cache for generated CSP policies
    private final Map<String, String> policyCache = new ConcurrentHashMap<>();
    
    // Cache for nonces (short-lived)
    private final Map<String, String> nonceCache = new ConcurrentHashMap<>();

    public CspPolicyManager(CspPolicyProperties cspProperties,
                           CorsProperties corsProperties,
                           @Value("${spring.profiles.active:local}") String activeEnvironment) {
        this.cspProperties = cspProperties;
        this.corsProperties = corsProperties;
        this.activeEnvironment = activeEnvironment;
        this.secureRandom = new SecureRandom();
        
        logger.info("CSP Policy Manager initialized for environment: {}", activeEnvironment);
        if (cspProperties.isEnabled()) {
            logger.info("CSP enforcement enabled with report-only mode: {}", cspProperties.isReportOnly());
        } else {
            logger.warn("CSP enforcement is DISABLED - this should only be used in development");
        }
    }

    /**
     * Generates the complete CSP policy header value for the current request.
     * 
     * @param requestPath The path of the current request
     * @param origin The origin of the request (can be null)
     * @return The complete CSP policy string
     */
    public String generateCspPolicy(String requestPath, String origin) {
        if (!cspProperties.isEnabled()) {
            return null;
        }

        String cacheKey = generateCacheKey(requestPath, origin);
        
        // Check cache first for performance
        String cachedPolicy = policyCache.get(cacheKey);
        if (cachedPolicy != null) {
            return cachedPolicy;
        }

        StringBuilder policy = new StringBuilder();
        CspPolicyProperties.PolicyDirectives directives = getEffectiveDirectives();

        // Build CSP directives
        addDirective(policy, "default-src", directives.getDefaultSrc());
        addDirective(policy, "script-src", enhanceScriptSrc(directives.getScriptSrc(), requestPath));
        addDirective(policy, "style-src", enhanceStyleSrc(directives.getStyleSrc(), requestPath));
        addDirective(policy, "img-src", directives.getImgSrc());
        addDirective(policy, "connect-src", enhanceConnectSrc(directives.getConnectSrc(), origin));
        addDirective(policy, "font-src", directives.getFontSrc());
        addDirective(policy, "object-src", directives.getObjectSrc());
        addDirective(policy, "media-src", directives.getMediaSrc());
        addDirective(policy, "child-src", directives.getChildSrc());
        addDirective(policy, "frame-src", directives.getFrameSrc());
        addDirective(policy, "frame-ancestors", directives.getFrameAncestors());
        addDirective(policy, "form-action", directives.getFormAction());
        addDirective(policy, "base-uri", directives.getBaseUri());
        addDirective(policy, "manifest-src", directives.getManifestSrc());
        addDirective(policy, "worker-src", directives.getWorkerSrc());

        // Add special directives
        if (directives.isUpgradeInsecureRequests() && isProductionEnvironment()) {
            addDirective(policy, "upgrade-insecure-requests", List.of());
        }

        if (directives.isBlockAllMixedContent()) {
            addDirective(policy, "block-all-mixed-content", List.of());
        }

        if (directives.isRequireSriFor() && !directives.getSriResourceTypes().isEmpty()) {
            addDirective(policy, "require-sri-for", directives.getSriResourceTypes());
        }

        // Add reporting directive if enabled
        if (cspProperties.isViolationReportingEnabled()) {
            addDirective(policy, "report-uri", List.of(cspProperties.getReportUri()));
        }

        String finalPolicy = policy.toString().trim();
        if (finalPolicy.endsWith(";")) {
            finalPolicy = finalPolicy.substring(0, finalPolicy.length() - 1);
        }

        // Cache the policy
        policyCache.put(cacheKey, finalPolicy);
        
        logger.debug("Generated CSP policy for path '{}' and origin '{}': {}", requestPath, origin, finalPolicy);
        return finalPolicy;
    }

    /**
     * Gets the appropriate CSP header name based on report-only mode.
     * 
     * @return The CSP header name
     */
    public String getCspHeaderName() {
        return cspProperties.isReportOnly() ? 
            "Content-Security-Policy-Report-Only" : 
            "Content-Security-Policy";
    }

    /**
     * Generates a secure nonce for inline scripts/styles.
     * 
     * @return A base64-encoded nonce
     */
    public String generateNonce() {
        if (!cspProperties.isNonceEnabled()) {
            return null;
        }

        byte[] nonceBytes = new byte[cspProperties.getNonceLength()];
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        
        // Store nonce with timestamp for cleanup (nonces should be single-use)
        nonceCache.put(nonce, String.valueOf(System.currentTimeMillis()));
        
        // Cleanup old nonces (keep only last 1000)
        if (nonceCache.size() > 1000) {
            cleanupOldNonces();
        }
        
        return nonce;
    }

    /**
     * Validates if a given CSP policy is compliant with security best practices.
     * 
     * @param policy The CSP policy to validate
     * @return ValidationResult containing issues and recommendations
     */
    public CspValidationResult validatePolicy(String policy) {
        CspValidationResult result = new CspValidationResult();
        
        if (policy == null || policy.trim().isEmpty()) {
            result.addError("CSP policy is empty or null");
            return result;
        }

        // Check for unsafe directives
        if (policy.contains("'unsafe-inline'")) {
            result.addWarning("Policy contains 'unsafe-inline' which reduces security");
        }

        if (policy.contains("'unsafe-eval'")) {
            result.addError("Policy contains 'unsafe-eval' which is a critical security risk");
        }

        // Check for wildcard usage
        if (policy.contains("*") && !policy.contains("*.")) {
            result.addWarning("Policy contains wildcard (*) which may be overly permissive");
        }

        // Check for data: URIs in script-src
        if (policy.contains("script-src") && policy.contains("data:")) {
            result.addError("data: URIs in script-src are not recommended for security");
        }

        // Check for missing essential directives
        String[] essentialDirectives = {"default-src", "script-src", "object-src", "frame-ancestors"};
        for (String directive : essentialDirectives) {
            if (!policy.contains(directive)) {
                result.addWarning("Missing recommended directive: " + directive);
            }
        }

        // Check for HTTPS enforcement in production
        if (isProductionEnvironment() && !policy.contains("upgrade-insecure-requests")) {
            result.addRecommendation("Consider adding 'upgrade-insecure-requests' for production");
        }

        return result;
    }

    /**
     * Gets the effective CSP directives for the current environment.
     */
    private CspPolicyProperties.PolicyDirectives getEffectiveDirectives() {
        // Check for environment-specific configuration
        CspPolicyProperties.PolicyDirectives envDirectives = 
            cspProperties.getEnvironmentPolicies().get(activeEnvironment);
        
        if (envDirectives != null) {
            logger.debug("Using environment-specific CSP directives for: {}", activeEnvironment);
            return envDirectives;
        }
        
        return cspProperties.getDirectives();
    }

    /**
     * Enhances script-src directive with nonces and environment-specific rules.
     */
    private List<String> enhanceScriptSrc(List<String> scriptSrc, String requestPath) {
        List<String> enhanced = scriptSrc.stream().collect(Collectors.toList());
        
        // Add nonce support if enabled
        if (cspProperties.isNonceEnabled()) {
            enhanced.add("'nonce-{nonce}'"); // Placeholder for actual nonce injection
        }
        
        // Development environment enhancements
        if (isDevelopmentEnvironment()) {
            // Allow localhost for development tools
            if (!enhanced.contains("localhost:*")) {
                enhanced.add("localhost:*");
            }
        }
        
        return enhanced;
    }

    /**
     * Enhances style-src directive with nonces and environment-specific rules.
     */
    private List<String> enhanceStyleSrc(List<String> styleSrc, String requestPath) {
        List<String> enhanced = styleSrc.stream().collect(Collectors.toList());
        
        // Add nonce support if enabled
        if (cspProperties.isNonceEnabled()) {
            enhanced.add("'nonce-{nonce}'"); // Placeholder for actual nonce injection
        }
        
        // Allow Google Fonts in all environments (common requirement)
        if (!enhanced.contains("fonts.googleapis.com")) {
            enhanced.add("fonts.googleapis.com");
        }
        
        return enhanced;
    }

    /**
     * Enhances connect-src directive with CORS origins and environment-specific rules.
     */
    private List<String> enhanceConnectSrc(List<String> connectSrc, String origin) {
        List<String> enhanced = connectSrc.stream().collect(Collectors.toList());
        
        // Add allowed CORS origins to connect-src
        for (String allowedOrigin : corsProperties.getAllowedOrigins()) {
            if (!allowedOrigin.equals("*") && !enhanced.contains(allowedOrigin)) {
                enhanced.add(allowedOrigin);
            }
        }
        
        // Add current origin if valid
        if (StringUtils.hasText(origin) && !enhanced.contains(origin) && isValidOrigin(origin)) {
            enhanced.add(origin);
        }
        
        return enhanced;
    }

    /**
     * Adds a directive to the CSP policy string.
     */
    private void addDirective(StringBuilder policy, String directiveName, List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            if (directiveName.equals("upgrade-insecure-requests") || 
                directiveName.equals("block-all-mixed-content")) {
                policy.append(directiveName).append("; ");
            }
            return;
        }
        
        policy.append(directiveName).append(" ");
        policy.append(String.join(" ", sources));
        policy.append("; ");
    }

    /**
     * Generates a cache key for CSP policies.
     */
    private String generateCacheKey(String requestPath, String origin) {
        return String.format("%s|%s|%s", activeEnvironment, 
                            requestPath != null ? requestPath : "", 
                            origin != null ? origin : "");
    }

    /**
     * Checks if the current environment is development.
     */
    private boolean isDevelopmentEnvironment() {
        return "local".equals(activeEnvironment) || "dev".equals(activeEnvironment);
    }

    /**
     * Checks if the current environment is production.
     */
    private boolean isProductionEnvironment() {
        return "prod".equals(activeEnvironment) || "production".equals(activeEnvironment);
    }

    /**
     * Validates if an origin is valid for CSP purposes.
     */
    private boolean isValidOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return false;
        }
        
        // Basic validation - should start with http/https and not be a suspicious origin
        return (origin.startsWith("http://") || origin.startsWith("https://")) &&
               !origin.contains("javascript:") &&
               !origin.contains("data:") &&
               !origin.equals("null");
    }

    /**
     * Cleans up old nonces from the cache.
     */
    private void cleanupOldNonces() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 300000; // 5 minutes
        
        nonceCache.entrySet().removeIf(entry -> {
            try {
                long nonceTime = Long.parseLong(entry.getValue());
                return (currentTime - nonceTime) > maxAge;
            } catch (NumberFormatException e) {
                return true; // Remove invalid entries
            }
        });
    }

    /**
     * Clears the policy cache (useful for configuration updates).
     */
    public void clearPolicyCache() {
        policyCache.clear();
        logger.info("CSP policy cache cleared");
    }

    /**
     * Gets cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        return Map.of(
            "policyCacheSize", policyCache.size(),
            "nonceCacheSize", nonceCache.size(),
            "activeEnvironment", activeEnvironment,
            "cspEnabled", cspProperties.isEnabled(),
            "reportOnlyMode", cspProperties.isReportOnly()
        );
    }
}
