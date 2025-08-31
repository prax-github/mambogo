package com.mambogo.gateway.config;

/**
 * Enhanced constants for advanced rate limiting configuration.
 * 
 * @author Prashant Sinha
 * @since SEC-05 Implementation, Enhanced in SEC-10
 */
public final class RateLimitConstants {

    // Key resolver bean names
    public static final String USER_KEY_RESOLVER = "userKeyResolver";
    public static final String IP_KEY_RESOLVER = "ipKeyResolver";
    public static final String ENDPOINT_USER_KEY_RESOLVER = "endpointUserKeyResolver";
    public static final String ENDPOINT_IP_KEY_RESOLVER = "endpointIpKeyResolver";
    
    // JWT claim names
    public static final String JWT_SUBJECT_CLAIM = "sub";
    public static final String JWT_PREFERRED_USERNAME_CLAIM = "preferred_username";
    
    // Rate limit key prefixes
    public static final String USER_RATE_LIMIT_KEY_PREFIX = "user:";
    public static final String IP_RATE_LIMIT_KEY_PREFIX = "ip:";
    public static final String ENDPOINT_USER_RATE_LIMIT_KEY_PREFIX = "endpoint:user:";
    public static final String ENDPOINT_IP_RATE_LIMIT_KEY_PREFIX = "endpoint:ip:";
    public static final String SERVICE_RATE_LIMIT_KEY_PREFIX = "service:";
    
    // Basic rate limit configuration (from SEC-05)
    public static final int DEFAULT_USER_RATE_LIMIT = 100; // requests per minute
    public static final int DEFAULT_IP_RATE_LIMIT = 1000; // requests per minute
    public static final int DEFAULT_REPLENISH_RATE = 10; // tokens per second
    public static final int DEFAULT_BURST_CAPACITY = 20; // maximum tokens
    
    // Rate limiter bean names
    public static final String USER_RATE_LIMITER = "userRateLimiter";
    public static final String IP_RATE_LIMITER = "ipRateLimiter";
    
    // Advanced rate limiter bean names (SEC-10)
    public static final String ORDERS_USER_RATE_LIMITER = "ordersUserRateLimiter";
    public static final String ORDERS_IP_RATE_LIMITER = "ordersIpRateLimiter";
    public static final String PAYMENTS_USER_RATE_LIMITER = "paymentsUserRateLimiter";
    public static final String PAYMENTS_IP_RATE_LIMITER = "paymentsIpRateLimiter";
    public static final String CART_USER_RATE_LIMITER = "cartUserRateLimiter";
    public static final String CART_IP_RATE_LIMITER = "cartIpRateLimiter";
    public static final String PRODUCTS_RATE_LIMITER = "productsRateLimiter";
    
    // Basic rate limit values (from SEC-05)
    public static final int USER_RATE_LIMIT_REQUESTS = 100;
    public static final int USER_RATE_LIMIT_DURATION_SECONDS = 60;
    public static final int IP_RATE_LIMIT_REQUESTS = 1000;
    public static final int IP_RATE_LIMIT_DURATION_SECONDS = 60;
    
    // Endpoint-specific rate limits (SEC-10)
    
    // Orders Service - More restrictive due to business impact
    public static final int ORDERS_USER_RATE_LIMIT_REQUESTS = 30; // 30 req/min per user
    public static final int ORDERS_IP_RATE_LIMIT_REQUESTS = 300; // 300 req/min per IP
    public static final int ORDERS_BURST_CAPACITY = 10;
    public static final int ORDERS_REPLENISH_RATE = 5; // 5 tokens per 10 seconds
    
    // Payments Service - Most restrictive due to financial operations
    public static final int PAYMENTS_USER_RATE_LIMIT_REQUESTS = 20; // 20 req/min per user
    public static final int PAYMENTS_IP_RATE_LIMIT_REQUESTS = 200; // 200 req/min per IP
    public static final int PAYMENTS_BURST_CAPACITY = 5;
    public static final int PAYMENTS_REPLENISH_RATE = 3; // 3 tokens per 10 seconds
    
    // Cart Service - Moderate limits for user experience
    public static final int CART_USER_RATE_LIMIT_REQUESTS = 60; // 60 req/min per user
    public static final int CART_IP_RATE_LIMIT_REQUESTS = 600; // 600 req/min per IP
    public static final int CART_BURST_CAPACITY = 20;
    public static final int CART_REPLENISH_RATE = 10; // 10 tokens per 10 seconds
    
    // Products Service - Most permissive for browsing
    public static final int PRODUCTS_IP_RATE_LIMIT_REQUESTS = 2000; // 2000 req/min per IP
    public static final int PRODUCTS_BURST_CAPACITY = 50;
    public static final int PRODUCTS_REPLENISH_RATE = 33; // 33 tokens per 10 seconds
    
    // Advanced configuration (SEC-10)
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;
    public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    public static final int CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS = 30;
    public static final double ADAPTIVE_THRESHOLD_FACTOR = 0.8; // Reduce limits to 80% under high load
    
    // Service-level rate limiting
    public static final int SERVICE_INTERNAL_RATE_LIMIT = 500; // requests per minute between services
    public static final int SERVICE_EXTERNAL_RATE_LIMIT = 1000; // requests per minute from external
    
    // Headers
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-Rate-Limit-Reset";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-Rate-Limit-Limit";
    public static final String RATE_LIMIT_WINDOW_HEADER = "X-Rate-Limit-Window";
    public static final String RATE_LIMIT_POLICY_HEADER = "X-Rate-Limit-Policy";
    
    // Error messages
    public static final String RATE_LIMIT_ERROR_CODE = "RATE_LIMIT_EXCEEDED";
    public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded. Please try again later.";
    public static final String CIRCUIT_BREAKER_ERROR_CODE = "CIRCUIT_BREAKER_OPEN";
    public static final String CIRCUIT_BREAKER_MESSAGE = "Service temporarily unavailable due to high error rate.";
    public static final String ADAPTIVE_RATE_LIMIT_MESSAGE = "Rate limit temporarily reduced due to high system load.";
    
    // Metrics names
    public static final String RATE_LIMIT_EXCEEDED_METRIC = "gateway.rate.limit.exceeded";
    public static final String RATE_LIMIT_ALLOWED_METRIC = "gateway.rate.limit.allowed";
    public static final String RATE_LIMIT_PROCESSING_TIME_METRIC = "gateway.rate.limit.processing.time";
    public static final String ENDPOINT_RATE_LIMIT_EXCEEDED_METRIC = "gateway.endpoint.rate.limit.exceeded";
    public static final String CIRCUIT_BREAKER_STATE_METRIC = "gateway.circuit.breaker.state";
    public static final String ADAPTIVE_RATE_LIMIT_METRIC = "gateway.adaptive.rate.limit.active";
    
    private RateLimitConstants() {
        // Utility class - no instantiation
    }
}