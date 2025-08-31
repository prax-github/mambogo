package com.mambogo.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Advanced key resolvers for endpoint-specific rate limiting.
 * Provides granular rate limiting based on endpoint and user/IP combination.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Configuration
public class AdvancedRateLimitKeyResolvers {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedRateLimitKeyResolvers.class);
    
    /**
     * Endpoint-aware user key resolver.
     * Creates keys in format: "endpoint:user:{endpoint}:{userId}"
     */
    @Bean(ENDPOINT_USER_KEY_RESOLVER)
    public KeyResolver endpointUserKeyResolver() {
        return exchange -> {
            String endpoint = extractEndpoint(exchange);
            
            return ReactiveSecurityContextHolder.getContext()
                .cast(org.springframework.security.core.context.SecurityContext.class)
                .map(securityContext -> securityContext.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> {
                    Jwt jwt = authentication.getToken();
                    String userId = extractUserId(jwt);
                    String key = ENDPOINT_USER_RATE_LIMIT_KEY_PREFIX + endpoint + ":" + userId;
                    
                    logger.debug("Generated endpoint user key: {} for endpoint: {} and user: {}", 
                               key, endpoint, userId);
                    return key;
                })
                .doOnError(error -> {
                    logger.debug("Authentication failed for endpoint user key resolver: {}", 
                               error.getMessage());
                })
                .onErrorReturn(ENDPOINT_USER_RATE_LIMIT_KEY_PREFIX + endpoint + ":anonymous");
        };
    }
    
    /**
     * Endpoint-aware IP key resolver.
     * Creates keys in format: "endpoint:ip:{endpoint}:{clientIp}"
     */
    @Bean(ENDPOINT_IP_KEY_RESOLVER)
    public KeyResolver endpointIpKeyResolver() {
        return exchange -> {
            String endpoint = extractEndpoint(exchange);
            String clientIp = extractClientIp(exchange);
            String key = ENDPOINT_IP_RATE_LIMIT_KEY_PREFIX + endpoint + ":" + clientIp;
            
            logger.debug("Generated endpoint IP key: {} for endpoint: {} and IP: {}", 
                       key, endpoint, clientIp);
            
            return Mono.just(key);
        };
    }
    
    /**
     * Extract endpoint identifier from request path.
     * Maps paths to logical endpoints for rate limiting.
     */
    private String extractEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        
        // Map paths to logical endpoints
        if (path.startsWith("/api/orders")) {
            return "orders";
        } else if (path.startsWith("/api/payments")) {
            return "payments";
        } else if (path.startsWith("/api/cart")) {
            return "cart";
        } else if (path.startsWith("/api/products") || path.startsWith("/api/catalog")) {
            return "products";
        } else {
            return "default";
        }
    }
    
    /**
     * Extract user ID from JWT token with fallback handling.
     */
    private String extractUserId(Jwt jwt) {
        try {
            // Try 'sub' claim first
            String userId = jwt.getClaimAsString(JWT_SUBJECT_CLAIM);
            if (userId != null && !userId.trim().isEmpty()) {
                return sanitizeUserId(userId);
            }
            
            // Fallback to 'preferred_username'
            userId = jwt.getClaimAsString(JWT_PREFERRED_USERNAME_CLAIM);
            if (userId != null && !userId.trim().isEmpty()) {
                return sanitizeUserId(userId);
            }
            
            // Final fallback
            return "unknown";
        } catch (Exception e) {
            logger.warn("Failed to extract user ID from JWT: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Extract client IP with proxy header support.
     */
    private String extractClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For header first
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // Take the first IP in case of multiple proxies
            String clientIp = xForwardedFor.split(",")[0].trim();
            if (!clientIp.isEmpty()) {
                return sanitizeIp(clientIp);
            }
        }
        
        // Check X-Real-IP header
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return sanitizeIp(xRealIp);
        }
        
        // Fallback to remote address
        String remoteAddress = exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
        
        return sanitizeIp(remoteAddress);
    }
    
    /**
     * Sanitize user ID to prevent Redis key injection.
     */
    private String sanitizeUserId(String userId) {
        if (userId == null) return "unknown";
        
        // Remove potentially dangerous characters and limit length
        String sanitized = userId.replaceAll("[^a-zA-Z0-9@._-]", "");
        
        // Limit length to prevent memory issues
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }
    
    /**
     * Sanitize IP address to prevent Redis key injection.
     */
    private String sanitizeIp(String ip) {
        if (ip == null) return "unknown";
        
        // Basic IP address validation and sanitization
        String sanitized = ip.replaceAll("[^0-9a-fA-F:.\\[\\]]", "");
        
        // Limit length for safety
        if (sanitized.length() > 45) { // Max IPv6 length
            sanitized = sanitized.substring(0, 45);
        }
        
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }
}
