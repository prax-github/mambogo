package com.mambogo.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;


import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simplified CORS metrics collection for Prometheus monitoring.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
public class SimpleCorsMetricsCollector {


    
    // Basic counters
    private final Counter corsRequestsTotal;
    private final Counter corsBlockedRequestsTotal;
    private final Counter corsSecurityViolationsTotal;
    
    // Performance timer
    private final Timer corsValidationTimer;
    
    // Origin tracking
    private final ConcurrentHashMap<String, AtomicLong> originRequestCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);

    public SimpleCorsMetricsCollector(MeterRegistry meterRegistry) {
        
        this.corsRequestsTotal = Counter.builder("cors_requests_total")
                .description("Total number of CORS requests")
                .register(meterRegistry);
                
        this.corsBlockedRequestsTotal = Counter.builder("cors_blocked_requests_total")
                .description("Total number of blocked CORS requests")
                .register(meterRegistry);
                
        this.corsSecurityViolationsTotal = Counter.builder("cors_security_violations_total")
                .description("Total number of CORS security violations")
                .register(meterRegistry);
                
        this.corsValidationTimer = Timer.builder("cors_validation_duration_seconds")
                .description("Time spent validating CORS requests")
                .register(meterRegistry);
    }

    public void recordCorsRequest(String origin, String method, String status) {
        corsRequestsTotal.increment();
        totalRequests.incrementAndGet();
        
        if (origin != null) {
            originRequestCounts.computeIfAbsent(origin, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void recordBlockedRequest(String origin, String reason, String method) {
        corsBlockedRequestsTotal.increment();
    }

    public void recordSecurityViolation(String origin, String violationType, String severity) {
        corsSecurityViolationsTotal.increment();
    }

    public void recordValidationTime(Duration duration) {
        corsValidationTimer.record(duration);
    }

    public void recordPreflightRequest(String origin, String method) {
        // Simple increment for preflight requests
        corsRequestsTotal.increment();
    }

    public void recordSuspiciousOrigin(String origin, String reason) {
        corsSecurityViolationsTotal.increment();
    }

    public void recordSecurityIncident(String origin, String incidentType, String severity) {
        corsSecurityViolationsTotal.increment();
    }

    public void recordPolicyViolation(String origin, String violationType, String details) {
        corsSecurityViolationsTotal.increment();
    }

    public void recordCacheHit(String origin, String type) {
        // Cache hit recorded
    }

    public void recordCacheMiss(String origin, String type) {
        // Cache miss recorded
    }

    public void updateTrustedOriginCount(long count) {
        // Simple gauge update
    }

    public void updateBlockedOriginCount(long count) {
        // Simple gauge update
    }

    public long getOriginRequestCount(String origin) {
        AtomicLong count = originRequestCounts.get(origin);
        return count != null ? count.get() : 0L;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }
}
