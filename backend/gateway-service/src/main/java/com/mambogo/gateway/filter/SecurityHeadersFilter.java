package com.mambogo.gateway.filter;

import com.mambogo.gateway.metrics.SecurityHeadersMetrics;
import com.mambogo.gateway.security.SecurityHeadersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;


/**
 * Unified security headers filter that consolidates all security headers
 * into a single, high-performance filter with comprehensive coverage.
 * 
 * This filter replaces duplicate header setting across multiple filters and provides:
 * - Centralized security headers management
 * - Environment-aware security policies
 * - Performance optimization through caching
 * - Integration with existing security components
 * - Comprehensive audit and monitoring
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);
    
    private final SecurityHeadersManager securityHeadersManager;
    private final SecurityHeadersMetrics securityHeadersMetrics;
    
    /**
     * High precedence to ensure security headers are applied early
     * but after authentication and CORS processing.
     */
    private static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    public SecurityHeadersFilter(SecurityHeadersManager securityHeadersManager, 
                               SecurityHeadersMetrics securityHeadersMetrics) {
        this.securityHeadersManager = securityHeadersManager;
        this.securityHeadersMetrics = securityHeadersMetrics;
        logger.info("SecurityHeadersFilter initialized with order: {}", FILTER_ORDER);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String origin = request.getHeaders().getOrigin();
        
        // Skip security headers for certain paths if needed
        if (shouldSkipSecurityHeaders(path, method)) {
            logger.debug("Skipping security headers for path: {} method: {}", path, method);
            return chain.filter(exchange);
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Generate and apply all security headers
            var securityHeaders = securityHeadersManager.generateAndApplySecurityHeaders(exchange, origin);
            
            // Log security headers application
            if (logger.isDebugEnabled()) {
                logger.debug("Applied {} security headers for {} {} from origin: {}", 
                           securityHeaders.size(), method, path, origin);
            }
            
            // Record metrics if monitoring is enabled
            recordSecurityHeadersMetrics(path, origin, method, securityHeaders.size(), startTime);
            
            // Continue with the filter chain
            return chain.filter(exchange);
            
        } catch (Exception e) {
            logger.error("Error applying security headers for {} {}: {}", 
                        method, path, e.getMessage(), e);
            
            // Continue with the request even if security headers fail
            // This ensures the application remains functional
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    /**
     * Determines if security headers should be skipped for this request.
     */
    private boolean shouldSkipSecurityHeaders(String path, String method) {
        // Skip for health checks and monitoring endpoints
        if (path.startsWith("/actuator/") || path.startsWith("/health")) {
            return true;
        }
        
        // Skip for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }
        
        // Skip for static resources if needed
        if (path.startsWith("/static/") || path.startsWith("/assets/")) {
            return true;
        }
        
        return false;
    }

    /**
     * Records metrics for security headers application.
     */
    private void recordSecurityHeadersMetrics(String path, String origin, String method, 
                                           int headersCount, long startTime) {
        long duration = System.nanoTime() - startTime;
        
        // Log performance metrics if processing takes significant time
        if (duration > 1_000_000) { // > 1ms
            logger.debug("Security headers processing took {}ms for {} {}", 
                        duration / 1_000_000, method, path);
        }
        
        // Record metrics using the security headers metrics system
        try {
            securityHeadersMetrics.recordProcessingDuration(path, origin, duration);
        } catch (Exception e) {
            logger.debug("Failed to record security headers metrics: {}", e.getMessage());
        }
    }



    /**
     * Gets the current security headers configuration summary.
     */
    public Map<String, Object> getConfigurationSummary() {
        return securityHeadersManager.getConfigurationSummary();
    }

    /**
     * Clears the security headers cache.
     */
    public void clearCache() {
        securityHeadersManager.clearCache();
    }

    /**
     * Validates the current security headers configuration.
     */
    public void validateConfiguration() {
        securityHeadersManager.validateConfiguration();
    }
}
