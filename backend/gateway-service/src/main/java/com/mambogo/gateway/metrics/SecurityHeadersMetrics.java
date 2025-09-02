package com.mambogo.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics collection for security headers monitoring and analysis.
 * 
 * This component provides:
 * - Security headers application metrics
 * - Validation and compliance metrics
 * - Performance impact metrics
 * - Security effectiveness metrics
 * - Integration with Prometheus monitoring
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Component
public class SecurityHeadersMetrics {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersMetrics.class);
    
    private final MeterRegistry meterRegistry;
    
    // Metrics counters
    private final Counter securityHeadersAppliedTotal;
    private final Counter securityHeadersValidationErrorsTotal;
    private final Counter securityHeadersValidationWarningsTotal;
    private final Counter securityHeadersProcessingErrorsTotal;
    
    // Metrics timers
    private final Timer securityHeadersProcessingDuration;
    private final Timer securityHeadersValidationDuration;
    
    // Metrics gauges
    private final Gauge securityHeadersCacheSize;
    private final Gauge securityHeadersComplianceScore;
    
    // Atomic counters for dynamic metrics
    private final AtomicLong totalHeadersApplied = new AtomicLong(0);
    private final AtomicLong totalValidationErrors = new AtomicLong(0);
    private final AtomicLong totalValidationWarnings = new AtomicLong(0);
    private final AtomicLong totalProcessingErrors = new AtomicLong(0);
    
    // Cache for dynamic tag-based metrics
    private final ConcurrentHashMap<String, Counter> pathBasedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> originBasedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> methodBasedCounters = new ConcurrentHashMap<>();

    public SecurityHeadersMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize base metrics
        this.securityHeadersAppliedTotal = Counter.builder("security_headers_applied_total")
            .description("Total number of security headers applied to responses")
            .register(meterRegistry);
            
        this.securityHeadersValidationErrorsTotal = Counter.builder("security_headers_validation_errors_total")
            .description("Total number of security headers validation errors")
            .register(meterRegistry);
            
        this.securityHeadersValidationWarningsTotal = Counter.builder("security_headers_validation_warnings_total")
            .description("Total number of security headers validation warnings")
            .register(meterRegistry);
            
        this.securityHeadersProcessingErrorsTotal = Counter.builder("security_headers_processing_errors_total")
            .description("Total number of security headers processing errors")
            .register(meterRegistry);
        
        // Initialize timers
        this.securityHeadersProcessingDuration = Timer.builder("security_headers_processing_duration_seconds")
            .description("Time taken to process and apply security headers")
            .register(meterRegistry);
            
        this.securityHeadersValidationDuration = Timer.builder("security_headers_validation_duration_seconds")
            .description("Time taken to validate security headers")
            .register(meterRegistry);
        
        // Initialize gauges
        this.securityHeadersCacheSize = Gauge.builder("security_headers_cache_size", this, SecurityHeadersMetrics::getCacheSize)
            .description("Current size of security headers cache")
            .register(meterRegistry);
            
        this.securityHeadersComplianceScore = Gauge.builder("security_headers_compliance_score", this, SecurityHeadersMetrics::getComplianceScore)
            .description("Current security headers compliance score percentage")
            .register(meterRegistry);
        
        logger.info("SecurityHeadersMetrics initialized with Prometheus integration");
        
        // Log initial gauge values to ensure fields are recognized as used
        logger.debug("Initial cache size gauge: {}", securityHeadersCacheSize.getId().getName());
        logger.debug("Initial compliance score gauge: {}", securityHeadersComplianceScore.getId().getName());
    }

    /**
     * Records security headers application metrics.
     */
    public void recordSecurityHeadersApplied(String path, String origin, String method, int headersCount) {
        try {
            // Increment base counter
            securityHeadersAppliedTotal.increment();
            totalHeadersApplied.incrementAndGet();
            
            // Record path-based metrics
            recordPathBasedMetrics(path, headersCount);
            
            // Record origin-based metrics
            recordOriginBasedMetrics(origin, headersCount);
            
            // Record method-based metrics
            recordMethodBasedMetrics(method, headersCount);
            
            logger.debug("Recorded security headers applied: {} headers for {} {} from {}", 
                        headersCount, method, path, origin);
                        
        } catch (Exception e) {
            logger.error("Error recording security headers applied metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Records security headers processing duration.
     */
    public void recordProcessingDuration(String path, String origin, long durationNanos) {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(securityHeadersProcessingDuration);
            
            // Record duration with tags
            Timer.builder("security_headers_processing_duration_seconds")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
                
        } catch (Exception e) {
            logger.error("Error recording processing duration metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Records security headers validation metrics.
     */
    public void recordValidationResult(String path, String origin, boolean isValid, 
                                     int errorCount, int warningCount, long durationNanos) {
        try {
            // Record validation duration
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(securityHeadersValidationDuration);
            
            // Record validation result with tags
            Timer.builder("security_headers_validation_duration_seconds")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .tag("valid", String.valueOf(isValid))
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
            
            // Record error and warning counts
            if (errorCount > 0) {
                securityHeadersValidationErrorsTotal.increment(errorCount);
                totalValidationErrors.addAndGet(errorCount);
            }
            
            if (warningCount > 0) {
                securityHeadersValidationWarningsTotal.increment(warningCount);
                totalValidationWarnings.addAndGet(warningCount);
            }
            
            logger.debug("Recorded validation result: valid={}, errors={}, warnings={} for {} from {}", 
                        isValid, errorCount, warningCount, path, origin);
                        
        } catch (Exception e) {
            logger.error("Error recording validation metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Records security headers processing errors.
     */
    public void recordProcessingError(String path, String origin, String errorType, String errorMessage) {
        try {
            securityHeadersProcessingErrorsTotal.increment();
            totalProcessingErrors.incrementAndGet();
            
            // Record error with tags
            Counter.builder("security_headers_processing_errors_total")
                .tag("path", sanitizePath(path))
                .tag("origin", sanitizeOrigin(origin))
                .tag("error_type", sanitizeErrorType(errorType))
                .register(meterRegistry)
                .increment();
                
            logger.debug("Recorded processing error: {} - {} for {} from {}", 
                        errorType, errorMessage, path, origin);
                        
        } catch (Exception e) {
            logger.error("Error recording processing error metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Records cache-related metrics.
     */
    public void recordCacheOperation(String operation, String cacheKey, boolean success) {
        try {
            Counter.builder("security_headers_cache_operations_total")
                .tag("operation", operation)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
                
            if (success) {
                Counter.builder("security_headers_cache_hits_total")
                    .tag("operation", operation)
                    .register(meterRegistry)
                    .increment();
            } else {
                Counter.builder("security_headers_cache_misses_total")
                    .tag("operation", operation)
                    .register(meterRegistry)
                    .increment();
            }
            
        } catch (Exception e) {
            logger.error("Error recording cache metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Records path-based metrics.
     */
    private void recordPathBasedMetrics(String path, int headersCount) {
        String sanitizedPath = sanitizePath(path);
        String counterKey = "path:" + sanitizedPath;
        
        Counter counter = pathBasedCounters.computeIfAbsent(counterKey, k -> 
            Counter.builder("security_headers_path_total")
                .tag("path", sanitizedPath)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    /**
     * Records origin-based metrics.
     */
    private void recordOriginBasedMetrics(String origin, int headersCount) {
        String sanitizedOrigin = sanitizeOrigin(origin);
        String counterKey = "origin:" + sanitizedOrigin;
        
        Counter counter = originBasedCounters.computeIfAbsent(counterKey, k -> 
            Counter.builder("security_headers_origin_total")
                .tag("origin", sanitizedOrigin)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    /**
     * Records method-based metrics.
     */
    private void recordMethodBasedMetrics(String method, int headersCount) {
        String sanitizedMethod = sanitizeMethod(method);
        String counterKey = "method:" + sanitizedMethod;
        
        Counter counter = methodBasedCounters.computeIfAbsent(counterKey, k -> 
            Counter.builder("security_headers_method_total")
                .tag("method", sanitizedMethod)
                .register(meterRegistry)
        );
        
        counter.increment();
    }

    /**
     * Sanitizes path for metrics tags.
     */
    private String sanitizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "unknown";
        }
        
        // Limit path length and remove sensitive parts
        String sanitized = path.replaceAll("[0-9a-f]{8,}", "id")
                              .replaceAll("/[0-9]+", "/id")
                              .substring(0, Math.min(path.length(), 50));
        
        return sanitized.isEmpty() ? "root" : sanitized;
    }

    /**
     * Sanitizes origin for metrics tags.
     */
    private String sanitizeOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return "unknown";
        }
        
        // Extract domain from origin
        try {
            if (origin.startsWith("http://") || origin.startsWith("https://")) {
                String domain = origin.split("://")[1].split("/")[0];
                return domain.length() > 30 ? domain.substring(0, 30) : domain;
            }
            return origin.length() > 30 ? origin.substring(0, 30) : origin;
        } catch (Exception e) {
            return "invalid";
        }
    }

    /**
     * Sanitizes method for metrics tags.
     */
    private String sanitizeMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "unknown";
        }
        
        return method.toUpperCase();
    }

    /**
     * Sanitizes error type for metrics tags.
     */
    private String sanitizeErrorType(String errorType) {
        if (errorType == null || errorType.trim().isEmpty()) {
            return "unknown";
        }
        
        return errorType.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    /**
     * Gets current cache size for gauge metric.
     */
    public double getCacheSize() {
        return pathBasedCounters.size() + originBasedCounters.size() + methodBasedCounters.size();
    }

    /**
     * Gets current compliance score for gauge metric.
     */
    public double getComplianceScore() {
        long totalChecks = totalValidationErrors.get() + totalValidationWarnings.get();
        if (totalChecks == 0) {
            return 100.0;
        }
        
        double errorWeight = 3.0; // Errors are 3x more important than warnings
        double totalWeightedScore = totalChecks * 100.0;
        double actualScore = (totalValidationErrors.get() * errorWeight * 100.0) + 
                           (totalValidationWarnings.get() * 100.0);
        
        return Math.max(0.0, 100.0 - (actualScore / totalWeightedScore));
    }

    /**
     * Gets metrics summary for monitoring and debugging.
     */
    public String getMetricsSummary() {
        return String.format(
            "Security Headers Metrics Summary - Applied: %d, Errors: %d, Warnings: %d, " +
            "Processing Errors: %d, Cache Size: %.0f, Compliance Score: %.1f%%",
            totalHeadersApplied.get(),
            totalValidationErrors.get(),
            totalValidationWarnings.get(),
            totalProcessingErrors.get(),
            getCacheSize(),
            getComplianceScore()
        );
    }

    /**
     * Resets all metrics counters (useful for testing).
     */
    public void resetMetrics() {
        totalHeadersApplied.set(0);
        totalValidationErrors.set(0);
        totalValidationWarnings.set(0);
        totalProcessingErrors.set(0);
        
        pathBasedCounters.clear();
        originBasedCounters.clear();
        methodBasedCounters.clear();
        
        logger.info("Security headers metrics reset");
    }
}
