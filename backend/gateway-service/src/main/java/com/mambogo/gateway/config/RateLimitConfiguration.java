package com.mambogo.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Configuration for rate limiting with different limits for users and IP addresses
 */
@Configuration
public class RateLimitConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfiguration.class);
    
    /**
     * Rate limiter for user-based requests (100 requests per minute)
     */
    @Bean(USER_RATE_LIMITER)
    public RedisRateLimiter userRateLimiter() {
        logger.info("Configuring user rate limiter: {} requests per {} seconds", 
                   USER_RATE_LIMIT_REQUESTS, USER_RATE_LIMIT_DURATION_SECONDS);
        return new RedisRateLimiter(USER_RATE_LIMIT_REQUESTS, USER_RATE_LIMIT_REQUESTS, USER_RATE_LIMIT_DURATION_SECONDS);
    }
    
    /**
     * Rate limiter for IP-based requests (1000 requests per minute)
     */
    @Bean(IP_RATE_LIMITER)
    public RedisRateLimiter ipRateLimiter() {
        logger.info("Configuring IP rate limiter: {} requests per {} seconds", 
                   IP_RATE_LIMIT_REQUESTS, IP_RATE_LIMIT_DURATION_SECONDS);
        return new RedisRateLimiter(IP_RATE_LIMIT_REQUESTS, IP_RATE_LIMIT_REQUESTS, IP_RATE_LIMIT_DURATION_SECONDS);
    }
    
    /**
     * Log rate limiting configuration on startup
     */
    @PostConstruct
    public void logConfiguration() {
        RateLimitMonitoring.logRateLimitConfiguration();
    }
}
