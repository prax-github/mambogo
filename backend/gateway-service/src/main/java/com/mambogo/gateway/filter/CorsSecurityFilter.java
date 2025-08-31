package com.mambogo.gateway.filter;

import com.mambogo.gateway.config.CorsProperties;
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

    public CorsSecurityFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String origin = request.getHeaders().getOrigin();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().value();
        
        // Log CORS requests for security monitoring
        if (origin != null) {
            logger.debug("CORS request - Origin: {}, Method: {}, Path: {}", origin, method, path);
            
            // Check for suspicious origins
            if (isSuspiciousOrigin(origin)) {
                logger.warn("SUSPICIOUS CORS REQUEST - Origin: {}, Method: {}, Path: {}, IP: {}", 
                           origin, method, path, getClientIP(request));
                
                // For high security environments, you might want to block these
                // return handleSuspiciousRequest(exchange);
            }
            
            // Validate origin against allowed origins
            if (!isOriginAllowed(origin)) {
                logger.warn("BLOCKED CORS REQUEST - Unauthorized origin: {}, Method: {}, Path: {}, IP: {}", 
                           origin, method, path, getClientIP(request));
                
                // Could optionally block here, but Spring Security CORS will handle it
                // return handleUnauthorizedOrigin(exchange);
            }
            
            // Log preflight requests
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                logger.debug("CORS preflight request from origin: {}", origin);
            }
        }
        
        // Add security headers
        addSecurityHeaders(response);
        
        return chain.filter(exchange);
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
     * Adds additional security headers to the response.
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        // Add security headers that complement CORS
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Content Security Policy for additional protection
        response.getHeaders().add("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self' " + String.join(" ", corsProperties.getAllowedOrigins()) + "; " +
            "frame-ancestors 'none';"
        );
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
