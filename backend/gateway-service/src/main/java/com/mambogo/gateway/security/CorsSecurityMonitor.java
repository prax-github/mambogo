package com.mambogo.gateway.security;

import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Advanced CORS security monitoring and threat detection.
 * 
 * Monitors CORS requests for security violations, suspicious patterns,
 * and potential attacks. Provides real-time threat detection and alerting.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsSecurityMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CorsSecurityMonitor.class);
    
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;
    
    // Security tracking
    private final ConcurrentHashMap<String, OriginSecurityProfile> originProfiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitTracker> rateLimitTrackers = new ConcurrentHashMap<>();
    
    // Security thresholds
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_BLOCKED_REQUESTS_PER_HOUR = 50;
    private static final double SUSPICIOUS_BLOCK_RATIO_THRESHOLD = 0.3; // 30%
    private static final int RAPID_FIRE_THRESHOLD = 20; // requests per 10 seconds
    
    // Pattern detection
    private static final Pattern SUSPICIOUS_ORIGIN_PATTERN = Pattern.compile(
        ".*(script|javascript|data|file|chrome-extension|moz-extension|null|undefined).*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern POTENTIAL_XSS_PATTERN = Pattern.compile(
        ".*(script|javascript|vbscript|onload|onerror|iframe).*", 
        Pattern.CASE_INSENSITIVE
    );

    public CorsSecurityMonitor(SimpleCorsMetricsCollector metricsCollector, ApplicationEventPublisher eventPublisher) {
        this.metricsCollector = metricsCollector;
        this.eventPublisher = eventPublisher;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Start periodic security analysis
        startSecurityAnalysis();
        
        logger.info("CORS Security Monitor initialized with thresholds: max_req/min={}, max_blocked/hour={}", 
                   MAX_REQUESTS_PER_MINUTE, MAX_BLOCKED_REQUESTS_PER_HOUR);
    }

    /**
     * Monitor a CORS request for security violations.
     */
    public SecurityAssessment monitorRequest(String origin, String method, String path, 
                                           String userAgent, String clientIP) {
        if (origin == null) {
            origin = "null";
        }
        
        SecurityAssessment assessment = new SecurityAssessment(origin);
        OriginSecurityProfile profile = getOrCreateProfile(origin);
        
        // Update request tracking
        profile.recordRequest(method, path, userAgent, clientIP);
        
        // Perform security checks
        checkSuspiciousOrigin(origin, assessment);
        checkRateLimit(origin, assessment);
        checkUserAgentAnomalies(userAgent, assessment);
        checkPathPatterns(path, assessment);
        checkRapidFireRequests(origin, assessment);
        
        // Update profile with assessment results
        if (assessment.hasViolations()) {
            profile.recordViolation(assessment.getHighestSeverity(), assessment.getSummary());
            
            // Publish security event
            CorsViolationEvent event = new CorsViolationEvent(
                origin, method, path, userAgent, clientIP, 
                assessment.getViolations(), assessment.getHighestSeverity()
            );
            eventPublisher.publishEvent(event);
            
            // Record metrics
            for (SecurityViolation violation : assessment.getViolations()) {
                metricsCollector.recordSecurityViolation(origin, violation.getType(), violation.getSeverity());
            }
        }
        
        return assessment;
    }

    /**
     * Monitor a blocked CORS request.
     */
    public void monitorBlockedRequest(String origin, String reason, String method, String path, String clientIP) {
        if (origin == null) {
            origin = "null";
        }
        
        OriginSecurityProfile profile = getOrCreateProfile(origin);
        profile.recordBlocked(reason);
        
        // Check for excessive blocking
        if (profile.getBlockedRequestsInLastHour() > MAX_BLOCKED_REQUESTS_PER_HOUR) {
            SecurityViolation violation = new SecurityViolation(
                "excessive_blocking", 
                "high", 
                "Origin has " + profile.getBlockedRequestsInLastHour() + " blocked requests in last hour"
            );
            
            CorsViolationEvent event = new CorsViolationEvent(
                origin, method, path, "", clientIP, 
                java.util.List.of(violation), "high"
            );
            eventPublisher.publishEvent(event);
            
            metricsCollector.recordSecurityViolation(origin, "excessive_blocking", "high");
        }
        
        logger.debug("Monitored blocked CORS request: origin={}, reason={}", origin, reason);
    }

    /**
     * Get security profile for an origin.
     */
    public OriginSecurityProfile getSecurityProfile(String origin) {
        return originProfiles.get(origin);
    }

    /**
     * Calculate security score for an origin (0.0 to 1.0, higher is better).
     */
    public double calculateSecurityScore(String origin) {
        OriginSecurityProfile profile = originProfiles.get(origin);
        if (profile == null) return 0.5; // Neutral score for unknown origins
        
        return profile.calculateSecurityScore();
    }

    /**
     * Check if an origin is currently considered high risk.
     */
    public boolean isHighRiskOrigin(String origin) {
        double score = calculateSecurityScore(origin);
        return score < 0.3; // Below 30% security score is high risk
    }

    /**
     * Get comprehensive security statistics.
     */
    public SecurityStatistics getSecurityStatistics() {
        int totalOrigins = originProfiles.size();
        long highRiskOrigins = originProfiles.values().stream()
                .mapToLong(profile -> profile.calculateSecurityScore() < 0.3 ? 1 : 0)
                .sum();
        
        long totalViolations = originProfiles.values().stream()
                .mapToLong(OriginSecurityProfile::getTotalViolations)
                .sum();
        
        long blockedRequests = originProfiles.values().stream()
                .mapToLong(OriginSecurityProfile::getTotalBlockedRequests)
                .sum();
        
        return new SecurityStatistics(totalOrigins, highRiskOrigins, totalViolations, blockedRequests);
    }

    /**
     * Start periodic security analysis.
     */
    private void startSecurityAnalysis() {
        // Security analysis every 5 minutes
        scheduler.scheduleAtFixedRate(this::performSecurityAnalysis, 5, 5, TimeUnit.MINUTES);
        
        // Cleanup old data every hour
        scheduler.scheduleAtFixedRate(this::cleanupOldData, 60, 60, TimeUnit.MINUTES);
    }

    /**
     * Perform comprehensive security analysis.
     */
    private void performSecurityAnalysis() {
        logger.debug("Performing CORS security analysis");
        
        SecurityStatistics stats = getSecurityStatistics();
        
        // Check for global security trends
        if (stats.getHighRiskOrigins() > stats.getTotalOrigins() * 0.1) { // More than 10% high risk
            logger.warn("High number of risky origins detected: {}/{}", 
                       stats.getHighRiskOrigins(), stats.getTotalOrigins());
        }
        
        // Analyze individual origins
        originProfiles.forEach((origin, profile) -> {
            double score = profile.calculateSecurityScore();
            if (score < 0.3) {
                logger.warn("High-risk origin detected: {} (score: {:.2f})", origin, score);
                
                // Publish high-risk origin event
                CorsViolationEvent event = new CorsViolationEvent(
                    origin, "", "", "", "", 
                    java.util.List.of(new SecurityViolation("high_risk_origin", "high", 
                        "Origin security score is " + String.format("%.2f", score))), 
                    "high"
                );
                eventPublisher.publishEvent(event);
            }
        });
        
        logger.info("Security analysis complete: {} origins, {} high-risk", 
                   stats.getTotalOrigins(), stats.getHighRiskOrigins());
    }

    /**
     * Get or create security profile for an origin.
     */
    private OriginSecurityProfile getOrCreateProfile(String origin) {
        return originProfiles.computeIfAbsent(origin, OriginSecurityProfile::new);
    }

    /**
     * Check for suspicious origin patterns.
     */
    private void checkSuspiciousOrigin(String origin, SecurityAssessment assessment) {
        if (SUSPICIOUS_ORIGIN_PATTERN.matcher(origin).matches()) {
            assessment.addViolation("suspicious_origin", "high", 
                "Origin matches suspicious pattern: " + origin);
            metricsCollector.recordSuspiciousOrigin(origin, "pattern_match");
        }
        
        if (POTENTIAL_XSS_PATTERN.matcher(origin).matches()) {
            assessment.addViolation("potential_xss", "critical", 
                "Origin contains potential XSS patterns: " + origin);
        }
    }

    /**
     * Check rate limiting violations.
     */
    private void checkRateLimit(String origin, SecurityAssessment assessment) {
        RateLimitTracker tracker = rateLimitTrackers.computeIfAbsent(origin, k -> new RateLimitTracker());
        tracker.recordRequest();
        
        if (tracker.getRequestsInLastMinute() > MAX_REQUESTS_PER_MINUTE) {
            assessment.addViolation("rate_limit_exceeded", "medium", 
                "Origin exceeded rate limit: " + tracker.getRequestsInLastMinute() + " requests/minute");
        }
    }

    /**
     * Check for user agent anomalies.
     */
    private void checkUserAgentAnomalies(String userAgent, SecurityAssessment assessment) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            assessment.addViolation("missing_user_agent", "low", "Missing User-Agent header");
            return;
        }
        
        // Check for suspicious user agents
        if (userAgent.toLowerCase().contains("bot") || 
            userAgent.toLowerCase().contains("crawler") ||
            userAgent.toLowerCase().contains("scanner")) {
            assessment.addViolation("suspicious_user_agent", "medium", 
                "Potentially automated client: " + userAgent);
        }
        
        // Check for extremely short or long user agents
        if (userAgent.length() < 10) {
            assessment.addViolation("short_user_agent", "low", "Unusually short User-Agent");
        } else if (userAgent.length() > 1000) {
            assessment.addViolation("long_user_agent", "medium", "Unusually long User-Agent");
        }
    }

    /**
     * Check for suspicious path patterns.
     */
    private void checkPathPatterns(String path, SecurityAssessment assessment) {
        if (path == null) return;
        
        // Check for path traversal attempts
        if (path.contains("../") || path.contains("..\\")) {
            assessment.addViolation("path_traversal", "high", "Path traversal attempt detected");
        }
        
        // Check for SQL injection patterns
        if (path.toLowerCase().matches(".*(union|select|insert|update|delete|drop|exec).*")) {
            assessment.addViolation("sql_injection_attempt", "high", "Potential SQL injection in path");
        }
    }

    /**
     * Check for rapid-fire request patterns.
     */
    private void checkRapidFireRequests(String origin, SecurityAssessment assessment) {
        RateLimitTracker tracker = rateLimitTrackers.get(origin);
        if (tracker != null && tracker.getRequestsInLastTenSeconds() > RAPID_FIRE_THRESHOLD) {
            assessment.addViolation("rapid_fire_requests", "high", 
                "Rapid-fire requests detected: " + tracker.getRequestsInLastTenSeconds() + " in 10 seconds");
        }
    }

    /**
     * Clean up old security data.
     */
    private void cleanupOldData() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        
        // Clean up origin profiles
        originProfiles.values().forEach(profile -> profile.cleanup(cutoff));
        originProfiles.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Clean up rate limit trackers
        rateLimitTrackers.values().forEach(tracker -> tracker.cleanup(cutoff));
        rateLimitTrackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        logger.debug("Security data cleanup complete. Active profiles: {}", originProfiles.size());
    }

    /**
     * Origin security profile tracking.
     */
    public static class OriginSecurityProfile {
        private final String origin;
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalViolations = new AtomicLong(0);
        private final AtomicLong totalBlockedRequests = new AtomicLong(0);
        private final ConcurrentHashMap<String, AtomicInteger> violationTypes = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> blockReasons = new ConcurrentHashMap<>();
        private volatile Instant lastActivity = Instant.now();
        private volatile Instant firstSeen = Instant.now();

        public OriginSecurityProfile(String origin) {
            this.origin = origin;
        }

        public void recordRequest(String method, String path, String userAgent, String clientIP) {
            totalRequests.incrementAndGet();
            lastActivity = Instant.now();
        }

        public void recordViolation(String severity, String details) {
            totalViolations.incrementAndGet();
            violationTypes.computeIfAbsent(severity, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public void recordBlocked(String reason) {
            totalBlockedRequests.incrementAndGet();
            blockReasons.computeIfAbsent(reason, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public double calculateSecurityScore() {
            long requests = totalRequests.get();
            if (requests == 0) return 0.5; // Neutral for no activity
            
            long violations = totalViolations.get();
            long blocked = totalBlockedRequests.get();
            
            // Base score calculation
            double violationRatio = (double) violations / requests;
            double blockRatio = (double) blocked / requests;
            
            // Score factors
            double violationScore = Math.max(0.0, 1.0 - (violationRatio * 2)); // Violations heavily penalize
            double blockScore = Math.max(0.0, 1.0 - blockRatio); // Blocks penalize
            
            // Age factor (newer origins are less trusted)
            long ageHours = ChronoUnit.HOURS.between(firstSeen, Instant.now());
            double ageFactor = Math.min(1.0, ageHours / 168.0); // Full trust after a week
            
            // Combined score
            return (violationScore * 0.5 + blockScore * 0.3 + ageFactor * 0.2);
        }

        public int getBlockedRequestsInLastHour() {
            // Simplified - would need time-based tracking for accurate implementation
            return (int) Math.min(totalBlockedRequests.get(), MAX_BLOCKED_REQUESTS_PER_HOUR);
        }

        public void cleanup(Instant cutoff) {
            // Cleanup logic for time-based data would go here
        }

        public boolean isEmpty() {
            return totalRequests.get() == 0;
        }

        // Getters
        public String getOrigin() { return origin; }
        public long getTotalRequests() { return totalRequests.get(); }
        public long getTotalViolations() { return totalViolations.get(); }
        public long getTotalBlockedRequests() { return totalBlockedRequests.get(); }
        public Instant getLastActivity() { return lastActivity; }
        public Instant getFirstSeen() { return firstSeen; }
    }

    /**
     * Rate limiting tracker for rapid request detection.
     */
    public static class RateLimitTracker {
        private final ConcurrentHashMap<Long, AtomicInteger> requestsByMinute = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, AtomicInteger> requestsByTenSeconds = new ConcurrentHashMap<>();

        public void recordRequest() {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            long currentTenSeconds = Instant.now().getEpochSecond() / 10;
            
            requestsByMinute.computeIfAbsent(currentMinute, k -> new AtomicInteger(0)).incrementAndGet();
            requestsByTenSeconds.computeIfAbsent(currentTenSeconds, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public int getRequestsInLastMinute() {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            return requestsByMinute.getOrDefault(currentMinute, new AtomicInteger(0)).get();
        }

        public int getRequestsInLastTenSeconds() {
            long currentTenSeconds = Instant.now().getEpochSecond() / 10;
            return requestsByTenSeconds.getOrDefault(currentTenSeconds, new AtomicInteger(0)).get();
        }

        public void cleanup(Instant cutoff) {
            long cutoffMinute = cutoff.getEpochSecond() / 60;
            long cutoffTenSeconds = cutoff.getEpochSecond() / 10;
            
            requestsByMinute.entrySet().removeIf(entry -> entry.getKey() < cutoffMinute);
            requestsByTenSeconds.entrySet().removeIf(entry -> entry.getKey() < cutoffTenSeconds);
        }

        public boolean isEmpty() {
            return requestsByMinute.isEmpty() && requestsByTenSeconds.isEmpty();
        }
    }

    /**
     * Security assessment result.
     */
    public static class SecurityAssessment {
        private final String origin;
        private final java.util.List<SecurityViolation> violations = new java.util.ArrayList<>();

        public SecurityAssessment(String origin) {
            this.origin = origin;
        }

        public void addViolation(String type, String severity, String details) {
            violations.add(new SecurityViolation(type, severity, details));
        }

        public boolean hasViolations() {
            return !violations.isEmpty();
        }

        public String getHighestSeverity() {
            return violations.stream()
                    .map(SecurityViolation::getSeverity)
                    .max((s1, s2) -> compareSeverity(s1, s2))
                    .orElse("low");
        }

        public String getSummary() {
            return violations.stream()
                    .map(v -> v.getType() + ":" + v.getSeverity())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }

        private int compareSeverity(String s1, String s2) {
            int priority1 = getSeverityPriority(s1);
            int priority2 = getSeverityPriority(s2);
            return Integer.compare(priority1, priority2);
        }

        private int getSeverityPriority(String severity) {
            switch (severity.toLowerCase()) {
                case "critical": return 4;
                case "high": return 3;
                case "medium": return 2;
                case "low": return 1;
                default: return 0;
            }
        }

        // Getters
        public String getOrigin() { return origin; }
        public java.util.List<SecurityViolation> getViolations() { return new java.util.ArrayList<>(violations); }
    }

    /**
     * Security statistics summary.
     */
    public static class SecurityStatistics {
        private final int totalOrigins;
        private final long highRiskOrigins;
        private final long totalViolations;
        private final long blockedRequests;

        public SecurityStatistics(int totalOrigins, long highRiskOrigins, long totalViolations, long blockedRequests) {
            this.totalOrigins = totalOrigins;
            this.highRiskOrigins = highRiskOrigins;
            this.totalViolations = totalViolations;
            this.blockedRequests = blockedRequests;
        }

        // Getters
        public int getTotalOrigins() { return totalOrigins; }
        public long getHighRiskOrigins() { return highRiskOrigins; }
        public long getTotalViolations() { return totalViolations; }
        public long getBlockedRequests() { return blockedRequests; }
    }
}
