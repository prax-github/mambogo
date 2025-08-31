package com.mambogo.gateway.monitoring;

import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CORS performance monitoring and optimization.
 * 
 * Monitors CORS request processing performance, identifies bottlenecks,
 * and provides insights for optimization.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsPerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CorsPerformanceMonitor.class);
    
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    // Performance tracking
    private final ConcurrentHashMap<String, PerformanceTracker> originPerformance = new ConcurrentHashMap<>();
    private final AtomicLong totalValidationTime = new AtomicLong(0);
    private final AtomicLong validationCount = new AtomicLong(0);
    private final AtomicReference<Duration> averageValidationTime = new AtomicReference<>(Duration.ZERO);
    
    // Performance thresholds
    private static final Duration WARNING_THRESHOLD = Duration.ofMillis(50);
    private static final Duration CRITICAL_THRESHOLD = Duration.ofMillis(100);
    private static final int CACHE_EFFICIENCY_THRESHOLD = 80; // 80%
    
    // Cache performance tracking
    private final AtomicLong preflightCacheHits = new AtomicLong(0);
    private final AtomicLong preflightCacheMisses = new AtomicLong(0);
    private final AtomicLong validationCacheHits = new AtomicLong(0);
    private final AtomicLong validationCacheMisses = new AtomicLong(0);

    public CorsPerformanceMonitor(SimpleCorsMetricsCollector metricsCollector, MeterRegistry meterRegistry) {
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Start periodic performance analysis
        startPeriodicAnalysis();
        
        logger.info("CORS Performance Monitor initialized with thresholds: warning={}ms, critical={}ms", 
                   WARNING_THRESHOLD.toMillis(), CRITICAL_THRESHOLD.toMillis());
    }

    /**
     * Record CORS validation timing.
     */
    public void recordValidationTime(String origin, Duration duration) {
        // Update global statistics
        totalValidationTime.addAndGet(duration.toNanos());
        validationCount.incrementAndGet();
        updateAverageValidationTime();
        
        // Track per-origin performance
        PerformanceTracker tracker = originPerformance.computeIfAbsent(origin, PerformanceTracker::new);
        tracker.recordValidation(duration);
        
        // Check for performance issues
        if (duration.compareTo(CRITICAL_THRESHOLD) > 0) {
            logger.warn("CRITICAL: CORS validation took {}ms for origin: {}", duration.toMillis(), origin);
            metricsCollector.recordSecurityViolation(origin, "performance_critical", "high");
        } else if (duration.compareTo(WARNING_THRESHOLD) > 0) {
            logger.warn("SLOW: CORS validation took {}ms for origin: {}", duration.toMillis(), origin);
        }
        
        // Record metrics
        metricsCollector.recordValidationTime(duration);
    }

    /**
     * Record preflight cache performance.
     */
    public void recordPreflightCacheHit(String origin) {
        preflightCacheHits.incrementAndGet();
        metricsCollector.recordCacheHit(origin, "preflight");
        
        PerformanceTracker tracker = originPerformance.computeIfAbsent(origin, PerformanceTracker::new);
        tracker.recordCacheHit("preflight");
    }

    /**
     * Record preflight cache miss.
     */
    public void recordPreflightCacheMiss(String origin) {
        preflightCacheMisses.incrementAndGet();
        metricsCollector.recordCacheMiss(origin, "preflight");
        
        PerformanceTracker tracker = originPerformance.computeIfAbsent(origin, PerformanceTracker::new);
        tracker.recordCacheMiss("preflight");
    }

    /**
     * Record validation cache performance.
     */
    public void recordValidationCacheHit(String origin) {
        validationCacheHits.incrementAndGet();
        metricsCollector.recordCacheHit(origin, "validation");
        
        PerformanceTracker tracker = originPerformance.computeIfAbsent(origin, PerformanceTracker::new);
        tracker.recordCacheHit("validation");
    }

    /**
     * Record validation cache miss.
     */
    public void recordValidationCacheMiss(String origin) {
        validationCacheMisses.incrementAndGet();
        metricsCollector.recordCacheMiss(origin, "validation");
        
        PerformanceTracker tracker = originPerformance.computeIfAbsent(origin, PerformanceTracker::new);
        tracker.recordCacheMiss("validation");
    }

    /**
     * Get performance statistics for an origin.
     */
    public OriginPerformanceStats getOriginPerformance(String origin) {
        PerformanceTracker tracker = originPerformance.get(origin);
        return tracker != null ? tracker.getStats() : new OriginPerformanceStats(origin);
    }

    /**
     * Get overall performance statistics.
     */
    public OverallPerformanceStats getOverallPerformance() {
        long totalHits = preflightCacheHits.get() + validationCacheHits.get();
        long totalMisses = preflightCacheMisses.get() + validationCacheMisses.get();
        double overallCacheEfficiency = calculateCacheEfficiency(totalHits, totalMisses);
        
        double preflightCacheEfficiency = calculateCacheEfficiency(
            preflightCacheHits.get(), preflightCacheMisses.get());
        
        double validationCacheEfficiency = calculateCacheEfficiency(
            validationCacheHits.get(), validationCacheMisses.get());
        
        return new OverallPerformanceStats(
            averageValidationTime.get(),
            validationCount.get(),
            overallCacheEfficiency,
            preflightCacheEfficiency,
            validationCacheEfficiency,
            originPerformance.size()
        );
    }

    /**
     * Get performance recommendations.
     */
    public PerformanceRecommendations getRecommendations() {
        OverallPerformanceStats stats = getOverallPerformance();
        PerformanceRecommendations recommendations = new PerformanceRecommendations();
        
        // Check validation time
        if (stats.getAverageValidationTime().compareTo(WARNING_THRESHOLD) > 0) {
            recommendations.addRecommendation("high_validation_time", 
                "Average CORS validation time is high (" + stats.getAverageValidationTime().toMillis() + "ms). " +
                "Consider optimizing origin validation logic or implementing caching.");
        }
        
        // Check cache efficiency
        if (stats.getOverallCacheEfficiency() < CACHE_EFFICIENCY_THRESHOLD) {
            recommendations.addRecommendation("low_cache_efficiency", 
                "Cache efficiency is low (" + String.format("%.1f%%", stats.getOverallCacheEfficiency()) + "). " +
                "Consider increasing cache TTL or reviewing cache invalidation policies.");
        }
        
        // Check preflight cache specifically
        if (stats.getPreflightCacheEfficiency() < CACHE_EFFICIENCY_THRESHOLD) {
            recommendations.addRecommendation("low_preflight_cache", 
                "Preflight cache efficiency is low. Consider increasing max-age for preflight responses.");
        }
        
        // Check for performance outliers
        long slowOrigins = originPerformance.values().stream()
                .mapToLong(tracker -> tracker.getStats().getAverageValidationTime().compareTo(WARNING_THRESHOLD) > 0 ? 1 : 0)
                .sum();
        
        if (slowOrigins > 0) {
            recommendations.addRecommendation("slow_origins", 
                slowOrigins + " origins have slow validation times. Review origin-specific optimizations.");
        }
        
        return recommendations;
    }

    /**
     * Start periodic performance analysis.
     */
    private void startPeriodicAnalysis() {
        scheduler.scheduleAtFixedRate(this::performPerformanceAnalysis, 5, 5, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::cleanupOldData, 60, 60, TimeUnit.MINUTES);
    }

    /**
     * Perform periodic performance analysis.
     */
    private void performPerformanceAnalysis() {
        logger.debug("Performing CORS performance analysis");
        
        OverallPerformanceStats stats = getOverallPerformance();
        PerformanceRecommendations recommendations = getRecommendations();
        
        // Log performance summary
        logger.info("CORS Performance Summary: avg_validation={}ms, cache_efficiency={:.1f}%, active_origins={}", 
                   stats.getAverageValidationTime().toMillis(), 
                   stats.getOverallCacheEfficiency(), 
                   stats.getActiveOrigins());
        
        // Log recommendations if any
        if (!recommendations.isEmpty()) {
            logger.warn("CORS Performance Recommendations: {}", recommendations.getSummary());
        }
        
        // Check for critical performance issues
        if (stats.getAverageValidationTime().compareTo(CRITICAL_THRESHOLD) > 0) {
            logger.error("CRITICAL: CORS validation performance is below acceptable thresholds");
        }
    }

    /**
     * Update average validation time.
     */
    private void updateAverageValidationTime() {
        long count = validationCount.get();
        if (count > 0) {
            long totalNanos = totalValidationTime.get();
            Duration average = Duration.ofNanos(totalNanos / count);
            averageValidationTime.set(average);
        }
    }

    /**
     * Calculate cache efficiency percentage.
     */
    private double calculateCacheEfficiency(long hits, long misses) {
        long total = hits + misses;
        if (total == 0) return 100.0;
        return (double) hits / total * 100.0;
    }

    /**
     * Clean up old performance data.
     */
    private void cleanupOldData() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour ago
        
        // Clean up per-origin trackers
        originPerformance.values().forEach(tracker -> tracker.cleanup(cutoff));
        
        // Remove empty trackers
        originPerformance.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        logger.debug("Performance data cleanup complete. Active origins: {}", originPerformance.size());
    }

    /**
     * Per-origin performance tracker.
     */
    private static class PerformanceTracker {
        private final String origin;
        private final AtomicLong validationCount = new AtomicLong(0);
        private final AtomicLong totalValidationTime = new AtomicLong(0);
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong cacheMisses = new AtomicLong(0);
        private final AtomicReference<Instant> lastActivity = new AtomicReference<>(Instant.now());

        public PerformanceTracker(String origin) {
            this.origin = origin;
        }

        public void recordValidation(Duration duration) {
            validationCount.incrementAndGet();
            totalValidationTime.addAndGet(duration.toNanos());
            lastActivity.set(Instant.now());
        }

        public void recordCacheHit(String type) {
            cacheHits.incrementAndGet();
            lastActivity.set(Instant.now());
        }

        public void recordCacheMiss(String type) {
            cacheMisses.incrementAndGet();
            lastActivity.set(Instant.now());
        }

        public OriginPerformanceStats getStats() {
            long count = validationCount.get();
            Duration averageTime = count > 0 ? 
                Duration.ofNanos(totalValidationTime.get() / count) : Duration.ZERO;
            
            double cacheEfficiency = calculateCacheEfficiency(cacheHits.get(), cacheMisses.get());
            
            return new OriginPerformanceStats(
                origin,
                averageTime,
                count,
                cacheEfficiency,
                lastActivity.get()
            );
        }

        public void cleanup(Instant cutoff) {
            // For now, we keep all data. Could implement sliding window cleanup here.
        }

        public boolean isEmpty() {
            return validationCount.get() == 0 && cacheHits.get() == 0 && cacheMisses.get() == 0;
        }

        private double calculateCacheEfficiency(long hits, long misses) {
            long total = hits + misses;
            if (total == 0) return 100.0;
            return (double) hits / total * 100.0;
        }
    }

    /**
     * Performance statistics for a specific origin.
     */
    public static class OriginPerformanceStats {
        private final String origin;
        private final Duration averageValidationTime;
        private final long validationCount;
        private final double cacheEfficiency;
        private final Instant lastActivity;

        public OriginPerformanceStats(String origin) {
            this(origin, Duration.ZERO, 0, 100.0, Instant.now());
        }

        public OriginPerformanceStats(String origin, Duration averageValidationTime, 
                                    long validationCount, double cacheEfficiency, Instant lastActivity) {
            this.origin = origin;
            this.averageValidationTime = averageValidationTime;
            this.validationCount = validationCount;
            this.cacheEfficiency = cacheEfficiency;
            this.lastActivity = lastActivity;
        }

        // Getters
        public String getOrigin() { return origin; }
        public Duration getAverageValidationTime() { return averageValidationTime; }
        public long getValidationCount() { return validationCount; }
        public double getCacheEfficiency() { return cacheEfficiency; }
        public Instant getLastActivity() { return lastActivity; }
    }

    /**
     * Overall performance statistics.
     */
    public static class OverallPerformanceStats {
        private final Duration averageValidationTime;
        private final long totalValidations;
        private final double overallCacheEfficiency;
        private final double preflightCacheEfficiency;
        private final double validationCacheEfficiency;
        private final int activeOrigins;

        public OverallPerformanceStats(Duration averageValidationTime, long totalValidations,
                                     double overallCacheEfficiency, double preflightCacheEfficiency,
                                     double validationCacheEfficiency, int activeOrigins) {
            this.averageValidationTime = averageValidationTime;
            this.totalValidations = totalValidations;
            this.overallCacheEfficiency = overallCacheEfficiency;
            this.preflightCacheEfficiency = preflightCacheEfficiency;
            this.validationCacheEfficiency = validationCacheEfficiency;
            this.activeOrigins = activeOrigins;
        }

        // Getters
        public Duration getAverageValidationTime() { return averageValidationTime; }
        public long getTotalValidations() { return totalValidations; }
        public double getOverallCacheEfficiency() { return overallCacheEfficiency; }
        public double getPreflightCacheEfficiency() { return preflightCacheEfficiency; }
        public double getValidationCacheEfficiency() { return validationCacheEfficiency; }
        public int getActiveOrigins() { return activeOrigins; }
    }

    /**
     * Performance recommendations.
     */
    public static class PerformanceRecommendations {
        private final ConcurrentHashMap<String, String> recommendations = new ConcurrentHashMap<>();

        public void addRecommendation(String key, String recommendation) {
            recommendations.put(key, recommendation);
        }

        public boolean isEmpty() {
            return recommendations.isEmpty();
        }

        public String getSummary() {
            return String.join("; ", recommendations.values());
        }

        public ConcurrentHashMap<String, String> getRecommendations() {
            return new ConcurrentHashMap<>(recommendations);
        }
    }
}
