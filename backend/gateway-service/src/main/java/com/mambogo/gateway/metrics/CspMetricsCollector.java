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

/**
 * Comprehensive metrics collector for Content Security Policy (CSP) monitoring.
 * Provides detailed metrics for CSP policy application, violations, performance,
 * and compliance tracking.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@Component
public class CspMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(CspMetricsCollector.class);

    private final MeterRegistry meterRegistry;
    
    // Dynamic metrics tracking
    private final ConcurrentHashMap<String, AtomicLong> activePoliciesPerOrigin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> violationsPerOrigin = new ConcurrentHashMap<>();
    private final AtomicLong totalPoliciesGenerated = new AtomicLong(0);
    private final AtomicLong totalViolationsReceived = new AtomicLong(0);

    public CspMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register dynamic gauges
        Gauge.builder("csp_active_policies_total", this, CspMetricsCollector::getTotalActivePolicies)
                .description("Total number of active CSP policies")
                .tag("component", "csp")
                .register(meterRegistry);
                
        Gauge.builder("csp_total_violations", this, CspMetricsCollector::getTotalViolations)
                .description("Total number of CSP violations across all origins")
                .tag("component", "csp")
                .register(meterRegistry);
        
        logger.info("CSP Metrics Collector initialized with comprehensive monitoring");
    }

    /**
     * Records that a CSP policy was applied to a response.
     */
    public void recordCspPolicyApplied(String path, String origin, String headerName) {
        Counter.builder("csp_policies_applied_total")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .tag("header_name", headerName)
                .register(meterRegistry)
                .increment();
        
        totalPoliciesGenerated.incrementAndGet();
        
        // Track per-origin policy application
        String originKey = sanitizeOrigin(origin);
        activePoliciesPerOrigin.computeIfAbsent(originKey, k -> new AtomicLong(0))
                               .incrementAndGet();
    }

    /**
     * Records a CSP policy validation error.
     */
    public void recordPolicyValidationError(String path, String origin) {
        Counter.builder("csp_policy_validation_errors_total")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a CSP processing error.
     */
    public void recordCspProcessingError(String path, String origin, String errorType) {
        Counter.builder("csp_processing_errors_total")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a CSP violation.
     */
    public void recordCspViolation(String violatedDirective, String blockedUri, 
                                  String documentUri, String origin) {
        Counter.builder("csp_violations_total")
                .tag("directive", violatedDirective)
                .tag("blocked_uri", sanitizeUri(blockedUri))
                .tag("document_uri", sanitizeUri(documentUri))
                .tag("origin", sanitizeOrigin(origin))
                .register(meterRegistry)
                .increment();
        
        totalViolationsReceived.incrementAndGet();
        
        // Track per-origin violations
        String originKey = sanitizeOrigin(origin);
        violationsPerOrigin.computeIfAbsent(originKey, k -> new AtomicLong(0))
                          .incrementAndGet();
    }

    /**
     * Records that a CSP violation report was received.
     */
    public void recordCspViolationReport(String origin, String userAgent) {
        Counter.builder("csp_violation_reports_total")
                .tag("origin", sanitizeOrigin(origin))
                .tag("user_agent", sanitizeUserAgent(userAgent))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records CSP processing time.
     */
    public void recordCspProcessingTime(String path, String origin, long durationNanos) {
        Timer.builder("csp_processing_duration_seconds")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNanos));
    }

    /**
     * Records that a CSP nonce was generated.
     */
    public void recordNonceGenerated(String path, String origin) {
        Counter.builder("csp_nonces_generated_total")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a CSP policy cache hit.
     */
    public void recordCspCacheHit(String cacheKey) {
        Counter.builder("csp_cache_hits_total")
                .tag("cache_key", sanitizeCacheKey(cacheKey))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a CSP policy cache miss.
     */
    public void recordCspCacheMiss(String cacheKey) {
        Counter.builder("csp_cache_misses_total")
                .tag("cache_key", sanitizeCacheKey(cacheKey))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records CSP policy validation results.
     */
    public void recordPolicyValidationResult(String path, String origin, boolean valid, 
                                           int errorCount, int warningCount, int recommendationCount) {
        String pathTag = sanitizePath(path);
        String originTag = sanitizeOrigin(origin);
        String validTag = String.valueOf(valid);
        
        if (errorCount > 0) {
            Counter.builder("csp_validation_errors_total")
                    .tag("path", pathTag)
                    .tag("origin", originTag)
                    .tag("valid", validTag)
                    .register(meterRegistry)
                    .increment(errorCount);
        }
        
        if (warningCount > 0) {
            Counter.builder("csp_validation_warnings_total")
                    .tag("path", pathTag)
                    .tag("origin", originTag)
                    .tag("valid", validTag)
                    .register(meterRegistry)
                    .increment(warningCount);
        }
        
        if (recommendationCount > 0) {
            Counter.builder("csp_validation_recommendations_total")
                    .tag("path", pathTag)
                    .tag("origin", originTag)
                    .tag("valid", validTag)
                    .register(meterRegistry)
                    .increment(recommendationCount);
        }
    }

    /**
     * Gets the total number of active policies across all origins.
     */
    public double getTotalActivePolicies() {
        return totalPoliciesGenerated.get();
    }

    /**
     * Gets the total number of violations across all origins.
     */
    public double getTotalViolations() {
        return totalViolationsReceived.get();
    }

    /**
     * Gets CSP metrics summary for monitoring dashboards.
     */
    public CspMetricsSummary getMetricsSummary() {
        return new CspMetricsSummary(
            totalPoliciesGenerated.get(),
            totalViolationsReceived.get(),
            activePoliciesPerOrigin.size(),
            violationsPerOrigin.size(),
            0.0, // cache hits (simplified for now)
            0.0  // cache misses (simplified for now)
        );
    }

    /**
     * Resets all dynamic counters (useful for testing or periodic cleanup).
     */
    public void resetDynamicCounters() {
        totalPoliciesGenerated.set(0);
        totalViolationsReceived.set(0);
        activePoliciesPerOrigin.clear();
        violationsPerOrigin.clear();
        
        logger.info("CSP metrics dynamic counters reset");
    }

    // Utility methods for sanitizing metric tags
    private String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        // Limit path length and remove sensitive information
        String sanitized = path.length() > 100 ? path.substring(0, 100) : path;
        return sanitized.replaceAll("[^a-zA-Z0-9/_-]", "_");
    }

    private String sanitizeOrigin(String origin) {
        if (origin == null || origin.isEmpty()) {
            return "unknown";
        }
        // Limit origin length and sanitize special characters
        String sanitized = origin.length() > 100 ? origin.substring(0, 100) : origin;
        return sanitized.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "unknown";
        }
        // Limit URI length and sanitize special characters
        String sanitized = uri.length() > 150 ? uri.substring(0, 150) : uri;
        return sanitized.replaceAll("[^a-zA-Z0-9._/-]", "_");
    }

    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        // Extract browser type from user agent for metrics
        if (userAgent.contains("Chrome")) return "chrome";
        if (userAgent.contains("Firefox")) return "firefox";
        if (userAgent.contains("Safari")) return "safari";
        if (userAgent.contains("Edge")) return "edge";
        return "other";
    }

    private String sanitizeCacheKey(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return "unknown";
        }
        return cacheKey.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * CSP metrics summary for dashboard display.
     */
    public static class CspMetricsSummary {
        private final long totalPoliciesGenerated;
        private final long totalViolations;
        private final int activeOriginsCount;
        private final int violatingOriginsCount;
        private final double cacheHits;
        private final double cacheMisses;

        public CspMetricsSummary(long totalPoliciesGenerated, long totalViolations,
                               int activeOriginsCount, int violatingOriginsCount,
                               double cacheHits, double cacheMisses) {
            this.totalPoliciesGenerated = totalPoliciesGenerated;
            this.totalViolations = totalViolations;
            this.activeOriginsCount = activeOriginsCount;
            this.violatingOriginsCount = violatingOriginsCount;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
        }

        // Getters
        public long getTotalPoliciesGenerated() { return totalPoliciesGenerated; }
        public long getTotalViolations() { return totalViolations; }
        public int getActiveOriginsCount() { return activeOriginsCount; }
        public int getViolatingOriginsCount() { return violatingOriginsCount; }
        public double getCacheHits() { return cacheHits; }
        public double getCacheMisses() { return cacheMisses; }
        
        public double getCacheHitRatio() {
            double total = cacheHits + cacheMisses;
            return total > 0 ? cacheHits / total : 0.0;
        }
    }
}
