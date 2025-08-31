package com.mambogo.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Advanced rate limiting configuration with endpoint-specific rate limiters.
 * Provides granular rate limiting policies for different service endpoints.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Configuration
public class AdvancedRateLimitConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedRateLimitConfiguration.class);
    
    // Orders Service Rate Limiters - Restrictive due to business impact
    
    /**
     * Rate limiter for orders endpoints - user-based (30 requests per minute).
     * More restrictive due to business impact of order operations.
     */
    @Bean(ORDERS_USER_RATE_LIMITER)
    public RedisRateLimiter ordersUserRateLimiter() {
        logger.info("Configuring orders user rate limiter: {} requests per {} seconds, burst: {}", 
                   ORDERS_USER_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, ORDERS_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            ORDERS_USER_RATE_LIMIT_REQUESTS, 
            ORDERS_BURST_CAPACITY, 
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    /**
     * Rate limiter for orders endpoints - IP-based (300 requests per minute).
     */
    @Bean(ORDERS_IP_RATE_LIMITER)
    public RedisRateLimiter ordersIpRateLimiter() {
        logger.info("Configuring orders IP rate limiter: {} requests per {} seconds, burst: {}", 
                   ORDERS_IP_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, ORDERS_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            ORDERS_IP_RATE_LIMIT_REQUESTS, 
            ORDERS_BURST_CAPACITY * 10, // Higher burst for IP-based
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    // Payments Service Rate Limiters - Most restrictive due to financial operations
    
    /**
     * Rate limiter for payments endpoints - user-based (20 requests per minute).
     * Most restrictive due to financial operations and fraud prevention.
     */
    @Bean(PAYMENTS_USER_RATE_LIMITER)
    public RedisRateLimiter paymentsUserRateLimiter() {
        logger.info("Configuring payments user rate limiter: {} requests per {} seconds, burst: {}", 
                   PAYMENTS_USER_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, PAYMENTS_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            PAYMENTS_USER_RATE_LIMIT_REQUESTS, 
            PAYMENTS_BURST_CAPACITY, 
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    /**
     * Rate limiter for payments endpoints - IP-based (200 requests per minute).
     */
    @Bean(PAYMENTS_IP_RATE_LIMITER)
    public RedisRateLimiter paymentsIpRateLimiter() {
        logger.info("Configuring payments IP rate limiter: {} requests per {} seconds, burst: {}", 
                   PAYMENTS_IP_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, PAYMENTS_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            PAYMENTS_IP_RATE_LIMIT_REQUESTS, 
            PAYMENTS_BURST_CAPACITY * 5, // Moderate burst for IP-based
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    // Cart Service Rate Limiters - Moderate limits for user experience
    
    /**
     * Rate limiter for cart endpoints - user-based (60 requests per minute).
     * Moderate limits to balance user experience with protection.
     */
    @Bean(CART_USER_RATE_LIMITER)
    public RedisRateLimiter cartUserRateLimiter() {
        logger.info("Configuring cart user rate limiter: {} requests per {} seconds, burst: {}", 
                   CART_USER_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, CART_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            CART_USER_RATE_LIMIT_REQUESTS, 
            CART_BURST_CAPACITY, 
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    /**
     * Rate limiter for cart endpoints - IP-based (600 requests per minute).
     */
    @Bean(CART_IP_RATE_LIMITER)
    public RedisRateLimiter cartIpRateLimiter() {
        logger.info("Configuring cart IP rate limiter: {} requests per {} seconds, burst: {}", 
                   CART_IP_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, CART_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            CART_IP_RATE_LIMIT_REQUESTS, 
            CART_BURST_CAPACITY * 3, // Higher burst for cart operations
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    // Products Service Rate Limiter - Most permissive for browsing
    
    /**
     * Rate limiter for products endpoints - IP-based (2000 requests per minute).
     * Most permissive to support product browsing and catalog operations.
     */
    @Bean(PRODUCTS_RATE_LIMITER)
    public RedisRateLimiter productsRateLimiter() {
        logger.info("Configuring products rate limiter: {} requests per {} seconds, burst: {}", 
                   PRODUCTS_IP_RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS, PRODUCTS_BURST_CAPACITY);
        
        return new RedisRateLimiter(
            PRODUCTS_IP_RATE_LIMIT_REQUESTS, 
            PRODUCTS_BURST_CAPACITY, 
            RATE_LIMIT_WINDOW_SECONDS
        );
    }
    
    /**
     * Log advanced rate limiting configuration on startup.
     */
    @PostConstruct
    public void logAdvancedConfiguration() {
        logger.info("=== Advanced Rate Limiting Configuration (SEC-10) ===");
        
        logger.info("Orders Service Rate Limits:");
        logger.info("  User: {} req/min, Burst: {}", ORDERS_USER_RATE_LIMIT_REQUESTS, ORDERS_BURST_CAPACITY);
        logger.info("  IP: {} req/min, Burst: {}", ORDERS_IP_RATE_LIMIT_REQUESTS, ORDERS_BURST_CAPACITY * 10);
        
        logger.info("Payments Service Rate Limits:");
        logger.info("  User: {} req/min, Burst: {}", PAYMENTS_USER_RATE_LIMIT_REQUESTS, PAYMENTS_BURST_CAPACITY);
        logger.info("  IP: {} req/min, Burst: {}", PAYMENTS_IP_RATE_LIMIT_REQUESTS, PAYMENTS_BURST_CAPACITY * 5);
        
        logger.info("Cart Service Rate Limits:");
        logger.info("  User: {} req/min, Burst: {}", CART_USER_RATE_LIMIT_REQUESTS, CART_BURST_CAPACITY);
        logger.info("  IP: {} req/min, Burst: {}", CART_IP_RATE_LIMIT_REQUESTS, CART_BURST_CAPACITY * 3);
        
        logger.info("Products Service Rate Limits:");
        logger.info("  IP: {} req/min, Burst: {}", PRODUCTS_IP_RATE_LIMIT_REQUESTS, PRODUCTS_BURST_CAPACITY);
        
        logger.info("Advanced Features:");
        logger.info("  Circuit Breaker Threshold: {} failures", CIRCUIT_BREAKER_FAILURE_THRESHOLD);
        logger.info("  Circuit Breaker Recovery: {} seconds", CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS);
        logger.info("  Adaptive Threshold Factor: {}%", (ADAPTIVE_THRESHOLD_FACTOR * 100));
        
        logger.info("=== End Advanced Rate Limiting Configuration ===");
    }
    
    /**
     * Get rate limiter configuration summary for monitoring.
     */
    public String getConfigurationSummary() {
        return String.format(
            "Advanced Rate Limiting - Orders: %d/%d req/min, Payments: %d/%d req/min, " +
            "Cart: %d/%d req/min, Products: %d req/min",
            ORDERS_USER_RATE_LIMIT_REQUESTS, ORDERS_IP_RATE_LIMIT_REQUESTS,
            PAYMENTS_USER_RATE_LIMIT_REQUESTS, PAYMENTS_IP_RATE_LIMIT_REQUESTS,
            CART_USER_RATE_LIMIT_REQUESTS, CART_IP_RATE_LIMIT_REQUESTS,
            PRODUCTS_IP_RATE_LIMIT_REQUESTS
        );
    }
}
