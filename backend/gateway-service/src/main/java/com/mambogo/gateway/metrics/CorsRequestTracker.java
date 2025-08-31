package com.mambogo.gateway.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced CORS request tracking and pattern analysis.
 * 
 * Tracks request patterns, identifies anomalies, and provides
 * detailed analytics for CORS request behavior.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsRequestTracker {

    private static final Logger logger = LoggerFactory.getLogger(CorsRequestTracker.class);
    
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    // Request tracking windows
    private static final int TRACKING_WINDOW_MINUTES = 60;
    private static final int ANALYSIS_INTERVAL_MINUTES = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 10000;
    
    // Request tracking data structures
    private final ConcurrentHashMap<String, OriginTracker> originTrackers = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<RequestEvent> recentRequests = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalRequestCount = new AtomicLong(0);

    public CorsRequestTracker(SimpleCorsMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start periodic analysis
        startPeriodicAnalysis();
        
        logger.info("CORS Request Tracker initialized with {}-minute tracking window", TRACKING_WINDOW_MINUTES);
    }

    /**
     * Track a CORS request event.
     */
    public void trackRequest(String origin, String method, String path, String userAgent, String clientIP) {
        if (origin == null) {
            origin = "null";
        }
        
        Instant now = Instant.now();
        RequestEvent event = new RequestEvent(origin, method, path, userAgent, clientIP, now);
        
        // Add to recent requests queue
        recentRequests.offer(event);
        
        // Track per-origin statistics
        OriginTracker tracker = originTrackers.computeIfAbsent(origin, OriginTracker::new);
        tracker.recordRequest(event);
        
        // Update global counters
        totalRequestCount.incrementAndGet();
        
        // Cleanup old events if queue gets too large
        if (recentRequests.size() > MAX_REQUESTS_PER_WINDOW) {
            cleanupOldRequests();
        }
        
        logger.debug("Tracked CORS request: origin={}, method={}, path={}", origin, method, path);
    }

    /**
     * Track a blocked CORS request.
     */
    public void trackBlockedRequest(String origin, String reason, String method, String path, String clientIP) {
        if (origin == null) {
            origin = "null";
        }
        
        OriginTracker tracker = originTrackers.computeIfAbsent(origin, OriginTracker::new);
        tracker.recordBlocked(reason, Instant.now());
        
        logger.info("Tracked blocked CORS request: origin={}, reason={}, method={}, path={}, ip={}", 
                   origin, reason, method, path, clientIP);
    }

    /**
     * Analyze request patterns for a specific origin.
     */
    public OriginAnalysis analyzeOrigin(String origin) {
        OriginTracker tracker = originTrackers.get(origin);
        if (tracker == null) {
            return new OriginAnalysis(origin, 0, 0, 0, 0, false, "No data available");
        }
        
        return tracker.analyze();
    }

    /**
     * Get request rate for an origin (requests per minute).
     */
    public double getOriginRequestRate(String origin) {
        OriginTracker tracker = originTrackers.get(origin);
        return tracker != null ? tracker.getRequestRate() : 0.0;
    }

    /**
     * Detect if an origin shows suspicious behavior.
     */
    public boolean isSuspiciousOrigin(String origin) {
        OriginTracker tracker = originTrackers.get(origin);
        if (tracker == null) return false;
        
        OriginAnalysis analysis = tracker.analyze();
        return analysis.isSuspicious();
    }

    /**
     * Get comprehensive tracking statistics.
     */
    public TrackingStatistics getStatistics() {
        long totalRequests = totalRequestCount.get();
        int activeOrigins = originTrackers.size();
        int recentRequestCount = recentRequests.size();
        
        long suspiciousOriginCount = originTrackers.values().stream()
                .mapToLong(tracker -> tracker.analyze().isSuspicious() ? 1 : 0)
                .sum();
        
        return new TrackingStatistics(
            totalRequests,
            activeOrigins,
            recentRequestCount,
            suspiciousOriginCount,
            calculateAverageRequestRate()
        );
    }

    /**
     * Start periodic analysis of request patterns.
     */
    private void startPeriodicAnalysis() {
        scheduler.scheduleAtFixedRate(this::performPatternAnalysis, 
                                     ANALYSIS_INTERVAL_MINUTES, 
                                     ANALYSIS_INTERVAL_MINUTES, 
                                     TimeUnit.MINUTES);
        
        scheduler.scheduleAtFixedRate(this::cleanupOldData, 
                                     TRACKING_WINDOW_MINUTES, 
                                     TRACKING_WINDOW_MINUTES, 
                                     TimeUnit.MINUTES);
    }

    /**
     * Perform pattern analysis on all tracked origins.
     */
    private void performPatternAnalysis() {
        logger.debug("Performing periodic CORS request pattern analysis");
        
        int suspiciousCount = 0;
        int totalOrigins = originTrackers.size();
        
        for (OriginTracker tracker : originTrackers.values()) {
            OriginAnalysis analysis = tracker.analyze();
            
            if (analysis.isSuspicious()) {
                suspiciousCount++;
                logger.warn("Suspicious CORS activity detected: origin={}, reason={}", 
                           analysis.getOrigin(), analysis.getSuspiciousReason());
                
                metricsCollector.recordSuspiciousOrigin(analysis.getOrigin(), analysis.getSuspiciousReason());
            }
        }
        
        logger.info("Pattern analysis complete: {} suspicious origins out of {} total", 
                   suspiciousCount, totalOrigins);
    }

    /**
     * Clean up old request events and tracking data.
     */
    private void cleanupOldRequests() {
        Instant cutoff = Instant.now().minusSeconds(TRACKING_WINDOW_MINUTES * 60L);
        
        // Remove old requests from queue
        while (!recentRequests.isEmpty()) {
            RequestEvent event = recentRequests.peek();
            if (event != null && event.getTimestamp().isBefore(cutoff)) {
                recentRequests.poll();
            } else {
                break;
            }
        }
    }

    /**
     * Clean up old tracking data.
     */
    private void cleanupOldData() {
        logger.debug("Cleaning up old CORS tracking data");
        
        Instant cutoff = Instant.now().minusSeconds(TRACKING_WINDOW_MINUTES * 60L);
        
        // Clean up origin trackers
        originTrackers.values().forEach(tracker -> tracker.cleanupOldData(cutoff));
        
        // Remove trackers with no recent activity
        originTrackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        cleanupOldRequests();
        
        logger.debug("Cleanup complete. Active origins: {}, Recent requests: {}", 
                    originTrackers.size(), recentRequests.size());
    }

    /**
     * Calculate average request rate across all origins.
     */
    private double calculateAverageRequestRate() {
        if (originTrackers.isEmpty()) return 0.0;
        
        double totalRate = originTrackers.values().stream()
                .mapToDouble(OriginTracker::getRequestRate)
                .sum();
        
        return totalRate / originTrackers.size();
    }

    /**
     * Shutdown the tracker and cleanup resources.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("CORS Request Tracker shutdown complete");
    }

    /**
     * Individual request event.
     */
    public static class RequestEvent {
        private final String origin;
        private final String method;
        private final String path;
        private final String userAgent;
        private final String clientIP;
        private final Instant timestamp;

        public RequestEvent(String origin, String method, String path, String userAgent, String clientIP, Instant timestamp) {
            this.origin = origin;
            this.method = method;
            this.path = path;
            this.userAgent = userAgent;
            this.clientIP = clientIP;
            this.timestamp = timestamp;
        }

        // Getters
        public String getOrigin() { return origin; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getUserAgent() { return userAgent; }
        public String getClientIP() { return clientIP; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Per-origin tracking data.
     */
    public static class OriginTracker {
        private final String origin;
        private final ConcurrentLinkedQueue<RequestEvent> requests = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<BlockedEvent> blockedRequests = new ConcurrentLinkedQueue<>();
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong blockedCount = new AtomicLong(0);

        public OriginTracker(String origin) {
            this.origin = origin;
        }

        public void recordRequest(RequestEvent event) {
            requests.offer(event);
            totalRequests.incrementAndGet();
        }

        public void recordBlocked(String reason, Instant timestamp) {
            blockedRequests.offer(new BlockedEvent(reason, timestamp));
            blockedCount.incrementAndGet();
        }

        public double getRequestRate() {
            return (double) requests.size() / TRACKING_WINDOW_MINUTES;
        }

        public OriginAnalysis analyze() {
            int requestCount = requests.size();
            int blockedCountValue = blockedRequests.size();
            double requestRate = getRequestRate();
            double blockRatio = requestCount > 0 ? (double) blockedCountValue / requestCount : 0.0;
            
            // Determine if suspicious
            boolean suspicious = false;
            String reason = "";
            
            if (requestRate > 100) { // More than 100 requests per minute
                suspicious = true;
                reason = "High request rate: " + String.format("%.2f", requestRate) + " req/min";
            } else if (blockRatio > 0.5) { // More than 50% blocked
                suspicious = true;
                reason = "High block ratio: " + String.format("%.2f%%", blockRatio * 100);
            } else if (hasRapidFirePattern()) {
                suspicious = true;
                reason = "Rapid-fire request pattern detected";
            }
            
            return new OriginAnalysis(origin, requestCount, blockedCountValue, requestRate, blockRatio, suspicious, reason);
        }

        public boolean isEmpty() {
            return requests.isEmpty() && blockedRequests.isEmpty();
        }

        public void cleanupOldData(Instant cutoff) {
            // Remove old requests
            while (!requests.isEmpty()) {
                RequestEvent event = requests.peek();
                if (event != null && event.getTimestamp().isBefore(cutoff)) {
                    requests.poll();
                } else {
                    break;
                }
            }
            
            // Remove old blocked events
            while (!blockedRequests.isEmpty()) {
                BlockedEvent event = blockedRequests.peek();
                if (event != null && event.getTimestamp().isBefore(cutoff)) {
                    blockedRequests.poll();
                } else {
                    break;
                }
            }
        }

        private boolean hasRapidFirePattern() {
            if (requests.size() < 10) return false;
            
            // Check if there are bursts of requests in short time periods
            Instant now = Instant.now();
            Instant oneMinuteAgo = now.minusSeconds(60);
            
            long recentRequests = requests.stream()
                    .filter(event -> event.getTimestamp().isAfter(oneMinuteAgo))
                    .count();
            
            return recentRequests > 50; // More than 50 requests in last minute
        }

        private static class BlockedEvent {
            private final Instant timestamp;

            public BlockedEvent(String reason, Instant timestamp) {
                this.timestamp = timestamp;
            }


            public Instant getTimestamp() { return timestamp; }
        }
    }

    /**
     * Analysis results for an origin.
     */
    public static class OriginAnalysis {
        private final String origin;
        private final int requestCount;
        private final int blockedCount;
        private final double requestRate;
        private final double blockRatio;
        private final boolean suspicious;
        private final String suspiciousReason;

        public OriginAnalysis(String origin, int requestCount, int blockedCount, 
                            double requestRate, double blockRatio, boolean suspicious, String suspiciousReason) {
            this.origin = origin;
            this.requestCount = requestCount;
            this.blockedCount = blockedCount;
            this.requestRate = requestRate;
            this.blockRatio = blockRatio;
            this.suspicious = suspicious;
            this.suspiciousReason = suspiciousReason;
        }

        // Getters
        public String getOrigin() { return origin; }
        public int getRequestCount() { return requestCount; }
        public int getBlockedCount() { return blockedCount; }
        public double getRequestRate() { return requestRate; }
        public double getBlockRatio() { return blockRatio; }
        public boolean isSuspicious() { return suspicious; }
        public String getSuspiciousReason() { return suspiciousReason; }
    }

    /**
     * Overall tracking statistics.
     */
    public static class TrackingStatistics {
        private final long totalRequests;
        private final int activeOrigins;
        private final int recentRequests;
        private final long suspiciousOrigins;
        private final double averageRequestRate;

        public TrackingStatistics(long totalRequests, int activeOrigins, int recentRequests, 
                                long suspiciousOrigins, double averageRequestRate) {
            this.totalRequests = totalRequests;
            this.activeOrigins = activeOrigins;
            this.recentRequests = recentRequests;
            this.suspiciousOrigins = suspiciousOrigins;
            this.averageRequestRate = averageRequestRate;
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public int getActiveOrigins() { return activeOrigins; }
        public int getRecentRequests() { return recentRequests; }
        public long getSuspiciousOrigins() { return suspiciousOrigins; }
        public double getAverageRequestRate() { return averageRequestRate; }
    }
}
