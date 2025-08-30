package com.mambogo.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Objects;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Custom key resolvers for rate limiting based on user ID and IP address
 */
@Configuration
public class RateLimitKeyResolvers {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitKeyResolvers.class);
    
    /**
     * Key resolver for user-based rate limiting
     * Extracts user ID from JWT token for authenticated requests
     */
    @Bean(USER_KEY_RESOLVER)
    public KeyResolver userKeyResolver() {
        return exchange -> {
            return exchange.getPrincipal()
                    .cast(JwtAuthenticationToken.class)
                    .map(JwtAuthenticationToken::getToken)
                    .map(jwt -> {
                        // Try to get user ID from 'sub' claim first, then 'preferred_username'
                        String userId = jwt.getClaimAsString(JWT_SUBJECT_CLAIM);
                        if (userId == null || userId.trim().isEmpty()) {
                            userId = jwt.getClaimAsString(JWT_PREFERRED_USERNAME_CLAIM);
                        }
                        if (userId == null || userId.trim().isEmpty()) {
                            userId = "anonymous";
                        }
                        
                        String key = USER_RATE_LIMIT_KEY_PREFIX + userId;
                        logger.debug("User rate limit key resolved: {}", key);
                        return key;
                    })
                    .doOnError(error -> logger.warn("Failed to resolve user from JWT, using anonymous", error))
                    .onErrorReturn(USER_RATE_LIMIT_KEY_PREFIX + "anonymous");
        };
    }
    
    /**
     * Key resolver for IP-based rate limiting
     * Extracts client IP address from the request
     */
    @Bean(IP_KEY_RESOLVER)
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = getClientIp(exchange);
            String key = IP_RATE_LIMIT_KEY_PREFIX + clientIp;
            logger.debug("IP rate limit key resolved: {}", key);
            return Mono.just(key);
        };
    }
    
    /**
     * Extracts client IP address from the request, considering proxy headers
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // Take the first IP if there are multiple (in case of multiple proxies)
            String clientIp = xForwardedFor.split(",")[0].trim();
            logger.debug("Client IP from X-Forwarded-For: {}", clientIp);
            return clientIp;
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            logger.debug("Client IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }
        
        // Fallback to remote address
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String clientIp = Objects.requireNonNull(remoteAddress).getAddress().getHostAddress();
        logger.debug("Client IP from remote address: {}", clientIp);
        return clientIp;
    }
}
