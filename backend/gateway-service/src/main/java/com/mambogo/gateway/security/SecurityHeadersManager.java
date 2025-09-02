package com.mambogo.gateway.security;

import com.mambogo.gateway.config.SecurityHeadersProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized security headers manager that consolidates all security headers
 * into a single, configurable, and auditable system.
 * 
 * This manager provides:
 * - Comprehensive security headers generation
 * - Environment-aware security policies
 * - Header validation and compliance checking
 * - Performance optimization through caching
 * - Integration with existing security components
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Component
public class SecurityHeadersManager {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersManager.class);
    
    private final SecurityHeadersProperties properties;
    private final Map<String, Map<String, String>> headerCache = new ConcurrentHashMap<>();
    
    public SecurityHeadersManager(SecurityHeadersProperties properties) {
        this.properties = properties;
        logger.info("SecurityHeadersManager initialized with security level: {}", 
                   properties.getEffectiveSecurityLevel());
    }

    /**
     * Generates and applies all security headers to the response.
     * 
     * @param exchange The server web exchange
     * @param origin The request origin for context-aware headers
     * @return Map of generated security headers
     */
    public Map<String, String> generateAndApplySecurityHeaders(ServerWebExchange exchange, String origin) {
        if (!properties.isEnabled()) {
            logger.debug("Security headers are disabled");
            return Map.of();
        }

        long startTime = System.nanoTime();
        
        try {
            // Generate all security headers
            Map<String, String> securityHeaders = generateSecurityHeaders(exchange, origin);
            
            // Apply headers to response
            applySecurityHeaders(exchange, securityHeaders);
            
            // Log performance metrics
            long duration = System.nanoTime() - startTime;
            if (duration > 1_000_000) { // Log if > 1ms
                logger.debug("Security headers generation took {}ms", duration / 1_000_000);
            }
            
            return securityHeaders;
            
        } catch (Exception e) {
            logger.error("Error generating security headers: {}", e.getMessage(), e);
            return Map.of();
        }
    }

    /**
     * Generates all security headers based on configuration and context.
     */
    private Map<String, String> generateSecurityHeaders(ServerWebExchange exchange, String origin) {
        String cacheKey = generateCacheKey(exchange, origin);
        
        // Check cache first
        Map<String, String> cachedHeaders = headerCache.get(cacheKey);
        if (cachedHeaders != null) {
            return new HashMap<>(cachedHeaders);
        }

        // Generate new headers
        Map<String, String> headers = new HashMap<>();
        
        // 1. MIME Type Protection
        if (properties.getMimeTypeProtection().isEnabled()) {
            headers.put("X-Content-Type-Options", properties.getMimeTypeProtection().getValue());
        }
        
        // 2. Clickjacking Protection
        if (properties.getClickjackingProtection().isEnabled()) {
            headers.put("X-Frame-Options", properties.getClickjackingProtection().getXFrameOptions());
        }
        
        // 3. XSS Protection
        if (properties.getXssProtection().isEnabled()) {
            headers.put("X-XSS-Protection", properties.getXssProtection().getValue());
        }
        
        // 4. Referrer Policy
        if (properties.getReferrerPolicy().isEnabled()) {
            headers.put("Referrer-Policy", properties.getReferrerPolicy().getValue());
        }
        
        // 5. HTTPS Enforcement
        if (properties.getHttpsEnforcement().isEnabled() && isProductionEnvironment()) {
            headers.put("Strict-Transport-Security", generateHstsHeader());
        }
        
        // 6. Feature Control
        if (properties.getFeatureControl().isEnabled()) {
            headers.put("Permissions-Policy", generatePermissionsPolicyHeader());
        }
        
        // 7. Additional Security Headers
        if (properties.getAdditionalHeaders().isEnabled()) {
            addAdditionalSecurityHeaders(headers);
        }
        
        // 8. Remove Server Information
        if (properties.getAdditionalHeaders().isPoweredBy()) {
            headers.put("Server", "MamboGo Gateway");
        }
        
        // Cache the generated headers
        headerCache.put(cacheKey, new HashMap<>(headers));
        
        return headers;
    }

    /**
     * Applies security headers to the response.
     */
    private void applySecurityHeaders(ServerWebExchange exchange, Map<String, String> headers) {
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        
        headers.forEach((name, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                responseHeaders.set(name, value);
                logger.debug("Applied security header: {} = {}", name, value);
            }
        });
    }

    /**
     * Generates HSTS header value.
     */
    private String generateHstsHeader() {
        SecurityHeadersProperties.HttpsEnforcement https = properties.getHttpsEnforcement();
        StringBuilder hsts = new StringBuilder("max-age=").append(https.getMaxAge());
        
        if (https.isIncludeSubDomains()) {
            hsts.append("; includeSubDomains");
        }
        
        if (https.isPreload()) {
            hsts.append("; preload");
        }
        
        return hsts.toString();
    }

    /**
     * Generates Permissions-Policy header value.
     */
    private String generatePermissionsPolicyHeader() {
        SecurityHeadersProperties.FeatureControl features = properties.getFeatureControl();
        StringBuilder policy = new StringBuilder();
        
        features.getDisabledFeatures().forEach(feature -> {
            if (policy.length() > 0) {
                policy.append(", ");
            }
            policy.append(feature).append("=()");
        });
        
        return policy.toString();
    }

    /**
     * Adds additional security headers.
     */
    private void addAdditionalSecurityHeaders(Map<String, String> headers) {
        SecurityHeadersProperties.AdditionalHeaders additional = properties.getAdditionalHeaders();
        
        if (additional.isDnsPrefetchControl()) {
            headers.put("X-DNS-Prefetch-Control", additional.getDnsPrefetchValue());
        }
        
        if (additional.isCrossDomainPolicies()) {
            headers.put("X-Permitted-Cross-Domain-Policies", additional.getCrossDomainValue());
        }
    }

    /**
     * Generates cache key for headers.
     */
    private String generateCacheKey(ServerWebExchange exchange, String origin) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String securityLevel = properties.getEffectiveSecurityLevel().name();
        
        return String.format("%s:%s:%s:%s", path, method, origin, securityLevel);
    }

    /**
     * Checks if current environment is production.
     */
    private boolean isProductionEnvironment() {
        return properties.getEffectiveSecurityLevel() == SecurityHeadersProperties.SecurityLevel.PRODUCTION;
    }

    /**
     * Gets the current security headers configuration summary.
     */
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("enabled", properties.isEnabled());
        summary.put("securityLevel", properties.getEffectiveSecurityLevel());
        summary.put("reportOnly", properties.isReportOnly());
        summary.put("cachedHeadersCount", headerCache.size());
        
        // Security features status
        Map<String, Boolean> features = new HashMap<>();
        features.put("mimeTypeProtection", properties.getMimeTypeProtection().isEnabled());
        features.put("clickjackingProtection", properties.getClickjackingProtection().isEnabled());
        features.put("xssProtection", properties.getXssProtection().isEnabled());
        features.put("referrerPolicy", properties.getReferrerPolicy().isEnabled());
        features.put("httpsEnforcement", properties.getHttpsEnforcement().isEnabled());
        features.put("featureControl", properties.getFeatureControl().isEnabled());
        features.put("additionalHeaders", properties.getAdditionalHeaders().isEnabled());
        
        summary.put("securityFeatures", features);
        return summary;
    }

    /**
     * Clears the header cache (useful for configuration updates).
     */
    public void clearCache() {
        int cacheSize = headerCache.size();
        headerCache.clear();
        logger.info("Security headers cache cleared ({} entries removed)", cacheSize);
    }

    /**
     * Validates current security headers configuration.
     */
    public void validateConfiguration() {
        try {
            properties.validateConfiguration();
            logger.debug("Security headers configuration validation passed");
        } catch (Exception e) {
            logger.error("Security headers configuration validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Gets the effective security level for the current environment.
     */
    public SecurityHeadersProperties.SecurityLevel getEffectiveSecurityLevel() {
        return properties.getEffectiveSecurityLevel();
    }
}
