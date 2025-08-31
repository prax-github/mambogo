package com.mambogo.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Advanced metrics collector for rate limiting monitoring and analytics.
 * Provides comprehensive insights into rate limiting patterns, performance,
 * and system behavior for operational excellence.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Component
public class AdvancedRateLimitMetrics {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedRateLimitMetrics.class);

    private final MeterRegistry meterRegistry;
    
    // Dynamic metrics tracking
    private final ConcurrentHashMap<String, AtomicLong> endpointRateLimitExceeded = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> endpointRateLimitAllowed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> circuitBreakerStates = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong totalRequestsProcessed = new AtomicLong(0);
    private final AtomicLong totalRateLimitChecks = new AtomicLong(0);
    private final AtomicLong adaptiveRateLimitActivations = new AtomicLong(0);

    public AdvancedRateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeGauges();
        logger.info("Advanced rate limiting metrics collector initialized");
    }

    /**
     * Initialize gauge metrics for real-time monitoring.
     */
    private void initializeGauges() {
        // Total requests processed gauge
        Gauge.builder("gateway.rate.limit.total.requests", totalRequestsProcessed, AtomicLong::get)
                .description("Total number of requests processed through rate limiting")
                .register(meterRegistry);

        // Total rate limit checks gauge
        Gauge.builder("gateway.rate.limit.total.checks", totalRateLimitChecks, AtomicLong::get)
                .description("Total number of rate limit checks performed")
                .register(meterRegistry);

        // Adaptive rate limiting activations gauge
        Gauge.builder("gateway.adaptive.rate.limit.activations", adaptiveRateLimitActivations, AtomicLong::get)
                .description("Number of times adaptive rate limiting was activated")
                .register(meterRegistry);

        // Active endpoint rate limiters gauge
        Gauge.builder("gateway.rate.limit.active.endpoints", endpointRateLimitAllowed, map -> map.size())
                .description("Number of active endpoint rate limiters")
                .register(meterRegistry);
    }

    /**
     * Record endpoint-specific rate limit exceeded event.
     */
    public void recordEndpointRateLimitExceeded(String endpoint, String origin, String keyType) {
        // Update counter for this endpoint
        endpointRateLimitExceeded.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record detailed counter with tags
        Counter.builder(ENDPOINT_RATE_LIMIT_EXCEEDED_METRIC)
                .description("Number of requests that exceeded endpoint-specific rate limits")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("origin", sanitizeTag(origin))
                .tag("key_type", sanitizeTag(keyType))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();

        totalRateLimitChecks.incrementAndGet();
        
        logger.debug("Recorded endpoint rate limit exceeded: endpoint={}, origin={}, keyType={}", 
                    endpoint, origin, keyType);
    }

    /**
     * Record endpoint-specific rate limit allowed event.
     */
    public void recordEndpointRateLimitAllowed(String endpoint, String origin, String keyType) {
        // Update counter for this endpoint
        endpointRateLimitAllowed.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record detailed counter with tags
        Counter.builder("gateway.endpoint.rate.limit.allowed")
                .description("Number of requests that passed endpoint-specific rate limits")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("origin", sanitizeTag(origin))
                .tag("key_type", sanitizeTag(keyType))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();

        totalRequestsProcessed.incrementAndGet();
        totalRateLimitChecks.incrementAndGet();
        
        logger.debug("Recorded endpoint rate limit allowed: endpoint={}, origin={}, keyType={}", 
                    endpoint, origin, keyType);
    }

    /**
     * Record rate limit processing time for performance monitoring.
     */
    public void recordRateLimitProcessingTime(String endpoint, String operation, long durationNanos) {
        Timer.builder("gateway.rate.limit.processing.duration")
                .description("Time taken to process rate limit checks")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("operation", sanitizeTag(operation))
                .tag("component", "gateway")
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNanos));
                
        logger.debug("Recorded rate limit processing time: endpoint={}, operation={}, duration={}ns", 
                    endpoint, operation, durationNanos);
    }

    /**
     * Record circuit breaker state change.
     */
    public void recordCircuitBreakerState(String endpoint, String state) {
        // Update state tracking
        circuitBreakerStates.put(endpoint, new AtomicLong(System.currentTimeMillis()));
        
        // Record state change event
        Counter.builder(CIRCUIT_BREAKER_STATE_METRIC)
                .description("Circuit breaker state changes")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("state", sanitizeTag(state))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();
                
        logger.info("Circuit breaker state changed: endpoint={}, state={}", endpoint, state);
    }

    /**
     * Record adaptive rate limiting activation.
     */
    public void recordAdaptiveRateLimitActivation(String endpoint, String reason, double adjustmentFactor) {
        adaptiveRateLimitActivations.incrementAndGet();
        
        // Record activation event with details
        Counter.builder(ADAPTIVE_RATE_LIMIT_METRIC)
                .description("Adaptive rate limiting activations")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("reason", sanitizeTag(reason))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();

        // Record adjustment factor as gauge (simplified - using counter instead)
        Counter.builder("gateway.adaptive.rate.limit.factor.updates")
                .description("Adaptive rate limiting factor updates")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("factor", String.valueOf(adjustmentFactor))
                .register(meterRegistry)
                .increment();
                
        logger.warn("Adaptive rate limiting activated: endpoint={}, reason={}, factor={}", 
                   endpoint, reason, adjustmentFactor);
    }

    /**
     * Record rate limit policy application.
     */
    public void recordRateLimitPolicyApplication(String endpoint, String policyType, String result) {
        Counter.builder("gateway.rate.limit.policy.applied")
                .description("Rate limit policy applications")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("policy_type", sanitizeTag(policyType))
                .tag("result", sanitizeTag(result))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();
                
        logger.debug("Rate limit policy applied: endpoint={}, policyType={}, result={}", 
                    endpoint, policyType, result);
    }

    /**
     * Record burst capacity utilization.
     */
    public void recordBurstCapacityUtilization(String endpoint, int used, int total) {
        double utilizationPercentage = total > 0 ? (double) used / total * 100 : 0;
        
        // Record burst utilization as counter for this event
        Counter.builder("gateway.rate.limit.burst.utilization.events")
                .description("Burst capacity utilization events")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("utilization", String.valueOf((int) utilizationPercentage))
                .register(meterRegistry)
                .increment();
                
        // Record high utilization events
        if (utilizationPercentage > 80) {
            Counter.builder("gateway.rate.limit.burst.high.utilization")
                    .description("High burst capacity utilization events")
                    .tag("endpoint", sanitizeTag(endpoint))
                    .tag("component", "gateway")
                    .register(meterRegistry)
                    .increment();
                    
            logger.warn("High burst capacity utilization: endpoint={}, used={}, total={}, utilization={}%", 
                       endpoint, used, total, utilizationPercentage);
        }
    }

    /**
     * Record rate limit violation patterns for security analysis.
     */
    public void recordViolationPattern(String endpoint, String pattern, String clientInfo) {
        Counter.builder("gateway.rate.limit.violation.pattern")
                .description("Rate limit violation patterns detected")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("pattern", sanitizeTag(pattern))
                .tag("component", "gateway")
                .register(meterRegistry)
                .increment();
                
        logger.warn("Rate limit violation pattern detected: endpoint={}, pattern={}, client={}", 
                   endpoint, pattern, clientInfo);
    }

    /**
     * Get endpoint-specific metrics summary.
     */
    public EndpointMetricsSummary getEndpointMetrics(String endpoint) {
        long exceeded = endpointRateLimitExceeded.getOrDefault(endpoint, new AtomicLong(0)).get();
        long allowed = endpointRateLimitAllowed.getOrDefault(endpoint, new AtomicLong(0)).get();
        
        return new EndpointMetricsSummary(endpoint, exceeded, allowed);
    }

    /**
     * Get overall rate limiting metrics summary.
     */
    public OverallMetricsSummary getOverallMetrics() {
        return new OverallMetricsSummary(
            totalRequestsProcessed.get(),
            totalRateLimitChecks.get(),
            adaptiveRateLimitActivations.get(),
            endpointRateLimitAllowed.size(),
            circuitBreakerStates.size()
        );
    }

    // These methods are referenced by gauges but may not be directly called in code

    /**
     * Sanitize tag values to prevent metric naming issues.
     */
    private String sanitizeTag(String tag) {
        if (tag == null) return "unknown";
        return tag.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    /**
     * Endpoint-specific metrics summary.
     */
    public static class EndpointMetricsSummary {
        private final String endpoint;
        private final long rateLimitExceeded;
        private final long rateLimitAllowed;

        public EndpointMetricsSummary(String endpoint, long rateLimitExceeded, long rateLimitAllowed) {
            this.endpoint = endpoint;
            this.rateLimitExceeded = rateLimitExceeded;
            this.rateLimitAllowed = rateLimitAllowed;
        }

        public String getEndpoint() { return endpoint; }
        public long getRateLimitExceeded() { return rateLimitExceeded; }
        public long getRateLimitAllowed() { return rateLimitAllowed; }
        public long getTotalRequests() { return rateLimitExceeded + rateLimitAllowed; }
        public double getSuccessRate() { 
            long total = getTotalRequests();
            return total > 0 ? (double) rateLimitAllowed / total * 100 : 0;
        }
    }

    /**
     * Overall metrics summary.
     */
    public static class OverallMetricsSummary {
        private final long totalRequestsProcessed;
        private final long totalRateLimitChecks;
        private final long adaptiveActivations;
        private final int activeEndpoints;
        private final int circuitBreakers;

        public OverallMetricsSummary(long totalRequestsProcessed, long totalRateLimitChecks, 
                                   long adaptiveActivations, int activeEndpoints, int circuitBreakers) {
            this.totalRequestsProcessed = totalRequestsProcessed;
            this.totalRateLimitChecks = totalRateLimitChecks;
            this.adaptiveActivations = adaptiveActivations;
            this.activeEndpoints = activeEndpoints;
            this.circuitBreakers = circuitBreakers;
        }

        public long getTotalRequestsProcessed() { return totalRequestsProcessed; }
        public long getTotalRateLimitChecks() { return totalRateLimitChecks; }
        public long getAdaptiveActivations() { return adaptiveActivations; }
        public int getActiveEndpoints() { return activeEndpoints; }
        public int getCircuitBreakers() { return circuitBreakers; }
    }
}
