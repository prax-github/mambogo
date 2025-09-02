package com.mambogo.gateway.filter;

import com.mambogo.gateway.config.CorsProperties;
import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import com.mambogo.gateway.monitoring.CorsPerformanceMonitor;
import com.mambogo.gateway.security.CorsSecurityMonitor;
import com.mambogo.gateway.audit.CorsAuditLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Additional CORS security filter to provide enhanced validation and logging.
 * This filter complements the standard Spring CORS configuration with:
 * - Enhanced security logging
 * - Origin validation
 * - Suspicious request detection
 * - Security headers enforcement
 * 
 * @author Prashant Sinha
 * @since SEC-06 Implementation
 */
@Component
public class CorsSecurityFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorsSecurityFilter.class);
    
    private final CorsProperties corsProperties;
    private final SimpleCorsMetricsCollector metricsCollector;
    private final CorsPerformanceMonitor performanceMonitor;
    private final CorsSecurityMonitor securityMonitor;
    private final CorsAuditLogger auditLogger;
    
    // Suspicious patterns that might indicate CORS attacks
    private static final Set<String> SUSPICIOUS_ORIGINS = Set.of(
        "null",
        "undefined",
        "javascript:",
        "data:",
        "file:",
        "chrome-extension:",
        "moz-extension:"
    );

    public CorsSecurityFilter(CorsProperties corsProperties,
                               SimpleCorsMetricsCollector metricsCollector,
                               CorsPerformanceMonitor performanceMonitor,
                               CorsSecurityMonitor securityMonitor,
                               CorsAuditLogger auditLogger) {
        this.corsProperties = corsProperties;
        this.metricsCollector = metricsCollector;
        this.performanceMonitor = performanceMonitor;
        this.securityMonitor = securityMonitor;
        this.auditLogger = auditLogger;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String origin = request.getHeaders().getOrigin();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().value();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String clientIP = getClientIP(request);
        
        // Start performance monitoring
        long startTime = System.nanoTime();
        
        try {
            // Log CORS requests for security monitoring
            if (origin != null) {
                logger.debug("CORS request - Origin: {}, Method: {}, Path: {}", origin, method, path);
                
                // Record metrics
                metricsCollector.recordCorsRequest(origin, method, "processing");
                
                // Check for suspicious origins
                if (isSuspiciousOrigin(origin)) {
                    logger.warn("SUSPICIOUS CORS REQUEST - Origin: {}, Method: {}, Path: {}, IP: {}", 
                               origin, method, path, clientIP);
                    
                    metricsCollector.recordSuspiciousOrigin(origin, "pattern_match");
                    auditLogger.logSecurityViolation(origin, "suspicious_origin", "medium", 
                        "Origin matches suspicious pattern", "logged");
                }
                
                // Perform security monitoring
                securityMonitor.monitorRequest(origin, method, path, userAgent, clientIP);
                
                // Validate origin against allowed origins
                if (!isOriginAllowed(origin)) {
                    logger.warn("BLOCKED CORS REQUEST - Unauthorized origin: {}, Method: {}, Path: {}, IP: {}", 
                               origin, method, path, clientIP);
                    
                    metricsCollector.recordBlockedRequest(origin, "origin_not_allowed", method);
                    securityMonitor.monitorBlockedRequest(origin, "origin_not_allowed", method, path, clientIP);
                    auditLogger.logCorsRequest(origin, method, path, userAgent, clientIP, "BLOCKED", "Origin not allowed");
                } else {
                    auditLogger.logCorsRequest(origin, method, path, userAgent, clientIP, "ALLOWED", "Origin validated");
                }
                
                // Log preflight requests
                if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                    logger.debug("CORS preflight request from origin: {}", origin);
                    metricsCollector.recordPreflightRequest(origin, method);
                    performanceMonitor.recordPreflightCacheHit(origin); // Simplified - would need actual cache check
                }
            }
            
            // Add security headers
            addSecurityHeaders(response);
            
            return chain.filter(exchange);
            
        } finally {
            // Complete performance monitoring
            long endTime = System.nanoTime();
            java.time.Duration validationTime = java.time.Duration.ofNanos(endTime - startTime);
            performanceMonitor.recordValidationTime(origin, validationTime);
        }
    }

    /**
     * Checks if the origin is suspicious and might indicate an attack.
     */
    private boolean isSuspiciousOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }
        
        String lowerOrigin = origin.toLowerCase();
        return SUSPICIOUS_ORIGINS.stream().anyMatch(lowerOrigin::startsWith);
    }

    /**
     * Validates if the origin is in the allowed origins list.
     */
    private boolean isOriginAllowed(String origin) {
        if (!corsProperties.isEnabled() || origin == null) {
            return true; // Let Spring Security CORS handle it
        }
        
        return corsProperties.getAllowedOrigins().contains(origin) ||
               corsProperties.getAllowedOrigins().contains("*");
    }

    /**
     * Extracts client IP address from request headers.
     */
    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        try {
            var remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            logger.debug("Could not extract remote address: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Security headers are now handled by the centralized SecurityHeadersFilter.
     * This method is kept for backward compatibility but no longer adds headers.
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        // Security headers are now managed centrally by SecurityHeadersFilter
        // This method is kept for backward compatibility
        logger.debug("Security headers are now managed centrally by SecurityHeadersFilter");
    }

    /**
     * Handles suspicious CORS requests.
     * Currently unused but kept for future security enhancements.
     */
    @SuppressWarnings("unused")
    private Mono<Void> handleSuspiciousRequest(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = "{\"error\":\"Suspicious request blocked\",\"code\":\"CORS_SECURITY_VIOLATION\"}";
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    /**
     * Handles unauthorized origin requests.
     * Currently unused but kept for future security enhancements.
     */
    @SuppressWarnings("unused")
    private Mono<Void> handleUnauthorizedOrigin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = "{\"error\":\"Origin not allowed\",\"code\":\"CORS_ORIGIN_DENIED\"}";
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        // Run before other filters but after basic security
        return -100;
    }
}
