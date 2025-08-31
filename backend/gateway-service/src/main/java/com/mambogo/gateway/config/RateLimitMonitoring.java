package com.mambogo.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;



/**
 * Monitoring and metrics configuration for rate limiting
 */
@Configuration
public class RateLimitMonitoring {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitMonitoring.class);
    
    /**
     * Counter for rate limit exceeded events
     */
    @Bean
    public Counter rateLimitExceededCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.rate.limit.exceeded")
                .description("Number of requests that exceeded rate limits")
                .tag("component", "gateway")
                .register(meterRegistry);
    }
    
    /**
     * Counter for rate limit allowed events
     */
    @Bean
    public Counter rateLimitAllowedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.rate.limit.allowed")
                .description("Number of requests that passed rate limits")
                .tag("component", "gateway")
                .register(meterRegistry);
    }
    
    /**
     * Timer for rate limit processing time
     */
    @Bean
    public Timer rateLimitProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("gateway.rate.limit.processing.time")
                .description("Time taken to process rate limit checks")
                .tag("component", "gateway")
                .register(meterRegistry);
    }
    
    /**
     * Logs rate limiting events for monitoring and debugging
     */
    public static void logRateLimitEvent(String eventType, ServerWebExchange exchange, 
                                       String keyType, String key, String remainingRequests) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = getClientIp(exchange);
        
        logger.info("Rate limit event: {} | Path: {} {} | Key Type: {} | Key: {} | Remaining: {} | Client IP: {}", 
                   eventType, method, path, keyType, key, remainingRequests, clientIp);
    }
    
    /**
     * Logs rate limiting configuration on startup
     */
    public static void logRateLimitConfiguration() {
        logger.info("Rate limiting configuration:");
        logger.info("  User rate limit: {} requests per {} seconds", 
                   RateLimitConstants.USER_RATE_LIMIT_REQUESTS, 
                   RateLimitConstants.USER_RATE_LIMIT_DURATION_SECONDS);
        logger.info("  IP rate limit: {} requests per {} seconds", 
                   RateLimitConstants.IP_RATE_LIMIT_REQUESTS, 
                   RateLimitConstants.IP_RATE_LIMIT_DURATION_SECONDS);
        logger.info("  Redis key prefixes - User: '{}', IP: '{}'", 
                   RateLimitConstants.USER_RATE_LIMIT_KEY_PREFIX,
                   RateLimitConstants.IP_RATE_LIMIT_KEY_PREFIX);
    }
    
    /**
     * Helper method to extract client IP for logging
     */
    private static String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp;
        }
        
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
