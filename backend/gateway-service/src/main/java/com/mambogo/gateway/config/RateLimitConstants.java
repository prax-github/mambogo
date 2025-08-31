package com.mambogo.gateway.config;

/**
 * Constants for rate limiting configuration.
 * 
 * @author Prashant Sinha
 * @since SEC-05 Implementation
 */
public final class RateLimitConstants {

    // Key resolver bean names
    public static final String USER_KEY_RESOLVER = "userKeyResolver";
    public static final String IP_KEY_RESOLVER = "ipKeyResolver";
    
    // JWT claim names
    public static final String JWT_SUBJECT_CLAIM = "sub";
    public static final String JWT_PREFERRED_USERNAME_CLAIM = "preferred_username";
    
    // Rate limit key prefixes
    public static final String USER_RATE_LIMIT_KEY_PREFIX = "user:";
    public static final String IP_RATE_LIMIT_KEY_PREFIX = "ip:";
    
    // Rate limit configuration
    public static final int DEFAULT_USER_RATE_LIMIT = 100; // requests per minute
    public static final int DEFAULT_IP_RATE_LIMIT = 1000; // requests per minute
    public static final int DEFAULT_REPLENISH_RATE = 10; // tokens per second
    public static final int DEFAULT_BURST_CAPACITY = 20; // maximum tokens
    
    // Rate limiter bean names
    public static final String USER_RATE_LIMITER = "userRateLimiter";
    public static final String IP_RATE_LIMITER = "ipRateLimiter";
    
    // Rate limit values
    public static final int USER_RATE_LIMIT_REQUESTS = 100;
    public static final int USER_RATE_LIMIT_DURATION_SECONDS = 60;
    public static final int IP_RATE_LIMIT_REQUESTS = 1000;
    public static final int IP_RATE_LIMIT_DURATION_SECONDS = 60;
    
    // Headers
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-Rate-Limit-Reset";
    
    // Error messages
    public static final String RATE_LIMIT_ERROR_CODE = "RATE_LIMIT_EXCEEDED";
    public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded. Please try again later.";
    
    private RateLimitConstants() {
        // Utility class - no instantiation
    }
}