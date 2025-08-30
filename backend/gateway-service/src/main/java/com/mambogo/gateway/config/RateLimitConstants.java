package com.mambogo.gateway.config;

/**
 * Constants for rate limiting configuration to avoid hardcoded strings
 */
public final class RateLimitConstants {
    
    // Rate limiting configuration values
    public static final int USER_RATE_LIMIT_REQUESTS = 100;
    public static final int USER_RATE_LIMIT_DURATION_SECONDS = 60;
    public static final int IP_RATE_LIMIT_REQUESTS = 1000;
    public static final int IP_RATE_LIMIT_DURATION_SECONDS = 60;
    
    // Redis key prefixes for rate limiting
    public static final String USER_RATE_LIMIT_KEY_PREFIX = "rate_limit:user:";
    public static final String IP_RATE_LIMIT_KEY_PREFIX = "rate_limit:ip:";
    
    // Rate limit header names
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    
    // Key resolver names
    public static final String USER_KEY_RESOLVER = "userKeyResolver";
    public static final String IP_KEY_RESOLVER = "ipKeyResolver";
    
    // Rate limiter names
    public static final String USER_RATE_LIMITER = "userRateLimiter";
    public static final String IP_RATE_LIMITER = "ipRateLimiter";
    
    // Error messages
    public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded";
    public static final String RATE_LIMIT_ERROR_CODE = "RATE_LIMIT_EXCEEDED";
    
    // JWT claim names
    public static final String JWT_SUBJECT_CLAIM = "sub";
    public static final String JWT_PREFERRED_USERNAME_CLAIM = "preferred_username";
    
    private RateLimitConstants() {
        // Utility class - prevent instantiation
    }
}
