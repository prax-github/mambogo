package com.mambogo.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collector for input sanitization operations, providing comprehensive
 * monitoring and observability for security threats and sanitization performance.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Component
public class SanitizationMetricsCollector {

    private final MeterRegistry meterRegistry;
    
    // Performance tracking
    private final AtomicLong totalSanitizationRequests = new AtomicLong(0);
    private final AtomicLong totalThreatDetections = new AtomicLong(0);
    private final AtomicLong totalBlockedRequests = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> endpointSanitizations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> originViolations = new ConcurrentHashMap<>();

    public SanitizationMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeGauges();
    }

    private void initializeGauges() {
        // Register gauges for real-time monitoring
        Gauge.builder("gateway_sanitization_total_requests", this, SanitizationMetricsCollector::getTotalSanitizationRequests)
            .description("Total number of sanitization requests processed")
            .register(meterRegistry);
            
        Gauge.builder("gateway_sanitization_total_threats", this, SanitizationMetricsCollector::getTotalThreatDetections)
            .description("Total number of threats detected")
            .register(meterRegistry);
            
        Gauge.builder("gateway_sanitization_total_blocked", this, SanitizationMetricsCollector::getTotalBlockedRequests)
            .description("Total number of requests blocked due to threats")
            .register(meterRegistry);
    }

    /**
     * Records a sanitization attempt
     */
    public void recordSanitizationAttempt(String endpoint, String origin, String policyName) {
        totalSanitizationRequests.incrementAndGet();
        endpointSanitizations.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        
        Counter.builder("gateway_sanitization_attempts_total")
            .description("Total sanitization attempts by endpoint and policy")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("policy", sanitizeTag(policyName))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records successful sanitization
     */
    public void recordSanitizationSuccess(String endpoint, String origin) {
        Counter.builder("gateway_sanitization_success_total")
            .description("Successful sanitization operations")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records sanitization errors
     */
    public void recordSanitizationError(String endpoint, String origin, String errorType) {
        Counter.builder("gateway_sanitization_errors_total")
            .description("Sanitization errors by type")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("error_type", sanitizeTag(errorType))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records sanitization processing duration
     */
    public void recordSanitizationDuration(String endpoint, String origin, long durationNanos) {
        Timer.builder("gateway_sanitization_duration_seconds")
            .description("Time spent on sanitization operations")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .register(meterRegistry)
            .record(Duration.ofNanos(durationNanos));
    }

    /**
     * Records threat detection
     */
    public void recordThreatDetected(String endpoint, String origin, String threatType) {
        totalThreatDetections.incrementAndGet();
        originViolations.computeIfAbsent(origin, k -> new AtomicLong(0)).incrementAndGet();
        
        Counter.builder("gateway_threats_detected_total")
            .description("Threats detected by type and endpoint")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("threat_type", sanitizeTag(threatType))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records blocked requests
     */
    public void recordBlockedRequest(String origin, String blockReason, String path) {
        totalBlockedRequests.incrementAndGet();
        
        Counter.builder("gateway_requests_blocked_total")
            .description("Requests blocked due to security violations")
            .tag("origin", sanitizeTag(origin))
            .tag("reason", sanitizeTag(blockReason))
            .tag("path", sanitizePath(path))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records threat analysis results
     */
    public void recordThreatAnalysis(String endpoint, String origin, int threatScore, 
                                   int threatCount, boolean blocked) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // Record threat score distribution
        Gauge.builder("gateway_threat_score_current", threatScore, score -> score)
            .description("Current threat score for monitoring")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .register(meterRegistry);
            
        // Record threat count
        Counter.builder("gateway_threat_analysis_total")
            .description("Threat analysis operations")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("blocked", String.valueOf(blocked))
            .register(meterRegistry)
            .increment();
            
        sample.stop(Timer.builder("gateway_threat_analysis_duration_seconds")
            .description("Time spent on threat analysis")
            .tag("endpoint", sanitizeTag(endpoint))
            .register(meterRegistry));
    }

    /**
     * Records request body size for monitoring
     */
    public void recordRequestBodySize(String endpoint, String origin, int bodySize) {
        Gauge.builder("gateway_request_body_size_bytes", bodySize, size -> size)
            .description("Request body size processed")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .register(meterRegistry);
    }

    /**
     * Records policy application
     */
    public void recordPolicyApplied(String endpoint, String policyName, boolean success) {
        Counter.builder("gateway_sanitization_policy_applied_total")
            .description("Sanitization policies applied")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("policy", sanitizeTag(policyName))
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records sanitization processing metrics
     */
    public void recordSanitizationProcessed(String endpoint, String origin, String method, boolean authenticated) {
        Counter.builder("gateway_sanitization_processed_total")
            .description("Requests processed through sanitization")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("method", sanitizeTag(method))
            .tag("authenticated", String.valueOf(authenticated))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records violation patterns for analysis
     */
    public void recordViolationPattern(String endpoint, String origin, String pattern, String severity) {
        Counter.builder("gateway_violation_patterns_total")
            .description("Security violation patterns detected")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("origin", sanitizeTag(origin))
            .tag("pattern", sanitizeTag(pattern))
            .tag("severity", sanitizeTag(severity))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records sanitization bypass operations
     */
    public void recordSanitizationBypassed(String path, String reason) {
        Counter.builder("gateway_sanitization_bypassed_total")
            .description("Requests that bypassed sanitization")
            .tag("path", sanitizePath(path))
            .tag("reason", sanitizeTag(reason))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Records performance metrics
     */
    public void recordPerformanceMetrics(String endpoint, long processingTimeNanos, 
                                       int inputSize, boolean threatDetected) {
        Timer.builder("gateway_sanitization_performance_seconds")
            .description("Overall sanitization performance")
            .tag("endpoint", sanitizeTag(endpoint))
            .tag("threat_detected", String.valueOf(threatDetected))
            .register(meterRegistry)
            .record(Duration.ofNanos(processingTimeNanos));
            
        if (inputSize > 0) {
            Gauge.builder("gateway_sanitization_input_size_bytes", inputSize, size -> size)
                .description("Input size processed for sanitization")
                .tag("endpoint", sanitizeTag(endpoint))
                .register(meterRegistry);
        }
    }

    /**
     * Records endpoint-specific statistics
     */
    public void recordEndpointStats(String endpoint, int requestCount, int threatCount, 
                                  double avgProcessingTime) {
        Gauge.builder("gateway_endpoint_request_count", requestCount, count -> count)
            .description("Request count by endpoint")
            .tag("endpoint", sanitizeTag(endpoint))
            .register(meterRegistry);
            
        Gauge.builder("gateway_endpoint_threat_count", threatCount, count -> count)
            .description("Threat count by endpoint")
            .tag("endpoint", sanitizeTag(endpoint))
            .register(meterRegistry);
            
        Gauge.builder("gateway_endpoint_avg_processing_time_seconds", avgProcessingTime, time -> time)
            .description("Average processing time by endpoint")
            .tag("endpoint", sanitizeTag(endpoint))
            .register(meterRegistry);
    }

    // Utility methods for getting current metrics values
    public long getTotalSanitizationRequests() {
        return totalSanitizationRequests.get();
    }

    public long getTotalThreatDetections() {
        return totalThreatDetections.get();
    }

    public long getTotalBlockedRequests() {
        return totalBlockedRequests.get();
    }

    public long getEndpointSanitizations(String endpoint) {
        AtomicLong count = endpointSanitizations.get(endpoint);
        return count != null ? count.get() : 0;
    }

    public long getOriginViolations(String origin) {
        AtomicLong count = originViolations.get(origin);
        return count != null ? count.get() : 0;
    }

    // Utility methods for tag sanitization
    private String sanitizeTag(String tag) {
        if (tag == null) {
            return "unknown";
        }
        
        // Remove special characters and limit length for metrics tags
        String sanitized = tag.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    private String sanitizePath(String path) {
        if (path == null) {
            return "/unknown";
        }
        
        // Extract main path components for metrics without exposing sensitive data
        String[] segments = path.split("/");
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return "/api/" + segments[2]; // e.g., "/api/products"
        }
        
        return path.length() > 50 ? path.substring(0, 50) : path;
    }
}
