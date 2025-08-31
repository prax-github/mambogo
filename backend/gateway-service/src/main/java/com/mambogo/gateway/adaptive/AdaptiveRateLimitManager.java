package com.mambogo.gateway.adaptive;

import com.mambogo.gateway.metrics.AdvancedRateLimitMetrics;
import com.mambogo.gateway.resilience.RateLimitCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Adaptive rate limiting manager that dynamically adjusts rate limits
 * based on system load, error rates, and traffic patterns.
 * Provides intelligent rate limiting that responds to changing conditions.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Component
public class AdaptiveRateLimitManager {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveRateLimitManager.class);

    private final AdvancedRateLimitMetrics metrics;
    private final RateLimitCircuitBreaker circuitBreaker;
    
    // Adaptive state tracking
    private final ConcurrentHashMap<String, AdaptiveState> endpointStates = new ConcurrentHashMap<>();
    private final AtomicLong lastSystemLoadCheck = new AtomicLong(System.currentTimeMillis());
    private volatile double systemLoadFactor = 1.0; // 1.0 = normal, <1.0 = reduced capacity
    
    public AdaptiveRateLimitManager(AdvancedRateLimitMetrics metrics, RateLimitCircuitBreaker circuitBreaker) {
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        logger.info("Adaptive rate limit manager initialized");
    }

    /**
     * Get the adjusted rate limit for an endpoint based on current conditions.
     */
    public int getAdjustedRateLimit(String endpoint, int baseRateLimit) {
        AdaptiveState state = endpointStates.computeIfAbsent(endpoint, k -> new AdaptiveState(endpoint));
        
        double adjustmentFactor = calculateAdjustmentFactor(endpoint, state);
        int adjustedLimit = (int) (baseRateLimit * adjustmentFactor);
        
        // Ensure minimum limit
        adjustedLimit = Math.max(adjustedLimit, baseRateLimit / 10); // Never go below 10% of base
        
        if (adjustmentFactor < 1.0) {
            logger.debug("Adaptive rate limit applied: endpoint={}, base={}, adjusted={}, factor={}", 
                        endpoint, baseRateLimit, adjustedLimit, adjustmentFactor);
            
            metrics.recordAdaptiveRateLimitActivation(endpoint, "load_based", adjustmentFactor);
        }
        
        return adjustedLimit;
    }

    /**
     * Record a request for adaptive tracking.
     */
    public void recordRequest(String endpoint, boolean rateLimited) {
        AdaptiveState state = endpointStates.computeIfAbsent(endpoint, k -> new AdaptiveState(endpoint));
        state.recordRequest(rateLimited);
        
        // Check for rapid rate limiting which may indicate need for adaptation
        if (rateLimited && state.getRateLimitedRequestsInLastMinute() > 50) {
            logger.warn("High rate limiting detected for endpoint: {}, rate limited requests in last minute: {}", 
                       endpoint, state.getRateLimitedRequestsInLastMinute());
        }
    }

    /**
     * Check if adaptive rate limiting should be applied for an endpoint.
     */
    public boolean shouldApplyAdaptiveRateLimit(String endpoint) {
        AdaptiveState state = endpointStates.get(endpoint);
        if (state == null) return false;
        
        // Check circuit breaker state
        String circuitState = circuitBreaker.getCircuitBreakerState(endpoint);
        if ("OPEN".equals(circuitState)) {
            return true;
        }
        
        // Check failure rate
        double failureRate = circuitBreaker.getFailureRate(endpoint);
        if (failureRate > 0.5) { // More than 50% failure rate
            return true;
        }
        
        // Check rate limiting frequency
        if (state.getRateLimitedRequestsInLastMinute() > 30) {
            return true;
        }
        
        // Check system load
        if (systemLoadFactor < 0.8) {
            return true;
        }
        
        return false;
    }

    /**
     * Calculate adjustment factor based on various conditions.
     */
    private double calculateAdjustmentFactor(String endpoint, AdaptiveState state) {
        double factor = 1.0;
        
        // Factor in system load
        factor *= systemLoadFactor;
        
        // Factor in circuit breaker state
        String circuitState = circuitBreaker.getCircuitBreakerState(endpoint);
        switch (circuitState) {
            case "OPEN":
                factor *= 0.1; // Severely reduce rate limit
                break;
            case "HALF_OPEN":
                factor *= 0.5; // Moderately reduce rate limit
                break;
            default:
                // CLOSED - no additional reduction
                break;
        }
        
        // Factor in current error rate
        double failureRate = circuitBreaker.getFailureRate(endpoint);
        if (failureRate > 0.2) {
            factor *= (1.0 - failureRate); // Reduce proportionally to failure rate
        }
        
        // Factor in recent rate limiting frequency
        int rateLimitedRequests = state.getRateLimitedRequestsInLastMinute();
        if (rateLimitedRequests > 20) {
            double rateLimitingFactor = Math.max(0.5, 1.0 - (rateLimitedRequests / 100.0));
            factor *= rateLimitingFactor;
        }
        
        // Apply minimum threshold factor
        factor = Math.max(factor, ADAPTIVE_THRESHOLD_FACTOR);
        
        return factor;
    }

    /**
     * Scheduled task to monitor system load and update adaptive parameters.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorSystemLoad() {
        try {
            // Simple system load monitoring (in production, this would integrate with system metrics)
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / totalMemory;
            
            // Calculate system load factor based on memory usage
            if (memoryUsagePercent > 0.9) {
                systemLoadFactor = 0.5; // High load - reduce to 50%
            } else if (memoryUsagePercent > 0.8) {
                systemLoadFactor = 0.7; // Medium load - reduce to 70%
            } else if (memoryUsagePercent > 0.7) {
                systemLoadFactor = 0.9; // Light load - reduce to 90%
            } else {
                systemLoadFactor = 1.0; // Normal load
            }
            
            lastSystemLoadCheck.set(System.currentTimeMillis());
            
            logger.debug("System load monitoring: memory usage={}%, load factor={}", 
                        memoryUsagePercent * 100, systemLoadFactor);
            
            // Clean up old endpoint states
            cleanupOldStates();
            
        } catch (Exception e) {
            logger.error("Error during system load monitoring", e);
        }
    }

    /**
     * Clean up old endpoint states to prevent memory leaks.
     */
    private void cleanupOldStates() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minutes ago
        
        endpointStates.entrySet().removeIf(entry -> {
            AdaptiveState state = entry.getValue();
            if (state.getLastActivity().isBefore(cutoff)) {
                logger.debug("Cleaning up old adaptive state for endpoint: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Get current system load factor for monitoring.
     */
    public double getSystemLoadFactor() {
        return systemLoadFactor;
    }

    /**
     * Get adaptive state summary for an endpoint.
     */
    public AdaptiveStateSummary getAdaptiveStateSummary(String endpoint) {
        AdaptiveState state = endpointStates.get(endpoint);
        if (state == null) {
            return new AdaptiveStateSummary(endpoint, 0, 0, 1.0, Instant.now());
        }
        
        return new AdaptiveStateSummary(
            endpoint,
            state.getTotalRequests(),
            state.getRateLimitedRequestsInLastMinute(),
            calculateAdjustmentFactor(endpoint, state),
            state.getLastActivity()
        );
    }

    /**
     * Adaptive state tracking for individual endpoints.
     */
    private static class AdaptiveState {
        @SuppressWarnings("unused") // Used for debugging and monitoring
        private final String endpoint;
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger rateLimitedRequests = new AtomicInteger(0);
        private final ConcurrentHashMap<Long, AtomicInteger> requestsByMinute = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, AtomicInteger> rateLimitedByMinute = new ConcurrentHashMap<>();
        private volatile Instant lastActivity = Instant.now();

        public AdaptiveState(String endpoint) {
            this.endpoint = endpoint;
        }

        public void recordRequest(boolean rateLimited) {
            totalRequests.incrementAndGet();
            lastActivity = Instant.now();
            
            long currentMinute = Instant.now().getEpochSecond() / 60;
            requestsByMinute.computeIfAbsent(currentMinute, k -> new AtomicInteger(0)).incrementAndGet();
            
            if (rateLimited) {
                rateLimitedRequests.incrementAndGet();
                rateLimitedByMinute.computeIfAbsent(currentMinute, k -> new AtomicInteger(0)).incrementAndGet();
            }
            
            // Clean up old data (keep only last 5 minutes)
            long cutoffMinute = currentMinute - 5;
            requestsByMinute.entrySet().removeIf(entry -> entry.getKey() < cutoffMinute);
            rateLimitedByMinute.entrySet().removeIf(entry -> entry.getKey() < cutoffMinute);
        }

        public int getTotalRequests() {
            return totalRequests.get();
        }

        public int getRateLimitedRequestsInLastMinute() {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            return rateLimitedByMinute.getOrDefault(currentMinute, new AtomicInteger(0)).get();
        }

        public Instant getLastActivity() {
            return lastActivity;
        }
    }

    /**
     * Adaptive state summary for monitoring.
     */
    public static class AdaptiveStateSummary {
        private final String endpoint;
        private final int totalRequests;
        private final int rateLimitedRequestsLastMinute;
        private final double adjustmentFactor;
        private final Instant lastActivity;

        public AdaptiveStateSummary(String endpoint, int totalRequests, int rateLimitedRequestsLastMinute, 
                                  double adjustmentFactor, Instant lastActivity) {
            this.endpoint = endpoint;
            this.totalRequests = totalRequests;
            this.rateLimitedRequestsLastMinute = rateLimitedRequestsLastMinute;
            this.adjustmentFactor = adjustmentFactor;
            this.lastActivity = lastActivity;
        }

        public String getEndpoint() { return endpoint; }
        public int getTotalRequests() { return totalRequests; }
        public int getRateLimitedRequestsLastMinute() { return rateLimitedRequestsLastMinute; }
        public double getAdjustmentFactor() { return adjustmentFactor; }
        public Instant getLastActivity() { return lastActivity; }
    }
}
