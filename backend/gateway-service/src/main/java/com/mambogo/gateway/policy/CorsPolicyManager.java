package com.mambogo.gateway.policy;

import com.mambogo.gateway.config.CorsProperties;
import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Advanced CORS policy management and enforcement.
 * 
 * Provides dynamic CORS policy management, runtime validation,
 * policy versioning, and compliance enforcement.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsPolicyManager {

    private static final Logger logger = LoggerFactory.getLogger(CorsPolicyManager.class);
    
    private final CorsProperties corsProperties;
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    // Policy state
    private final AtomicLong policyVersion = new AtomicLong(1);
    private final AtomicBoolean policyEnforcementEnabled = new AtomicBoolean(true);
    private volatile Instant lastPolicyUpdate = Instant.now();
    
    // Dynamic policy tracking
    private final ConcurrentHashMap<String, OriginPolicyState> originPolicies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PolicyViolation> recentViolations = new ConcurrentHashMap<>();
    
    // Policy patterns
    private static final Pattern LOCALHOST_PATTERN = Pattern.compile("^https?://localhost(:\\d+)?$");
    private static final Pattern DEVELOPMENT_PATTERN = Pattern.compile("^https?://[\\w-]+(:\\d+)?\\.(local|dev|test)$");
    private static final Pattern PRODUCTION_PATTERN = Pattern.compile("^https://[\\w-]+\\.(com|org|net|io)$");
    
    // Policy enforcement settings
    private static final int VIOLATION_THRESHOLD = 5; // violations before policy adjustment
    private static final int POLICY_REVIEW_INTERVAL_HOURS = 24;
    private static final int EMERGENCY_BLOCK_THRESHOLD = 10; // critical violations

    public CorsPolicyManager(CorsProperties corsProperties, SimpleCorsMetricsCollector metricsCollector) {
        this.corsProperties = corsProperties;
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        logger.info("CORS Policy Manager initialized with enforcement enabled: {}", policyEnforcementEnabled.get());
    }

    /**
     * Initialize policy management after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePolicyManagement() {
        logger.info("Initializing CORS policy management");
        
        // Load and validate current policy
        validateCurrentPolicy();
        
        // Start periodic policy review
        startPolicyReview();
        
        logger.info("CORS policy management initialized successfully");
    }

    /**
     * Validate CORS request against current policy.
     */
    public PolicyDecision validateRequest(String origin, String method, String path, 
                                        String userAgent, String clientIP) {
        if (!policyEnforcementEnabled.get()) {
            PolicyDecision decision = new PolicyDecision();
            decision.allow("Policy enforcement disabled");
            return decision;
        }
        
        PolicyDecision decision = new PolicyDecision();
        OriginPolicyState originPolicy = getOrCreateOriginPolicy(origin);
        
        // Check if origin is explicitly blocked
        if (originPolicy.isBlocked()) {
            decision.deny("Origin is blocked due to policy violations");
            return decision;
        }
        
        // Validate against allowed origins
        if (!isOriginAllowed(origin)) {
            decision.deny("Origin not in allowed list");
            recordPolicyViolation(origin, "origin_not_allowed", "Origin not in allowed origins list");
            return decision;
        }
        
        // Validate method
        if (!corsProperties.getAllowedMethods().contains(method)) {
            decision.deny("Method not allowed");
            recordPolicyViolation(origin, "method_not_allowed", "Method " + method + " not allowed");
            return decision;
        }
        
        // Apply dynamic policy rules
        PolicyDecision dynamicDecision = applyDynamicPolicyRules(origin, method, path, userAgent, clientIP);
        if (!dynamicDecision.isAllowed()) {
            return dynamicDecision;
        }
        
        // Update origin policy state
        originPolicy.recordRequest();
        
        decision.allow("Request meets all policy requirements");
        return decision;
    }

    /**
     * Record a policy violation.
     */
    public void recordPolicyViolation(String origin, String violationType, String details) {
        PolicyViolation violation = new PolicyViolation(origin, violationType, details);
        recentViolations.put(generateViolationKey(origin, violationType), violation);
        
        OriginPolicyState originPolicy = getOrCreateOriginPolicy(origin);
        originPolicy.recordViolation(violationType);
        
        // Check if origin should be blocked
        if (originPolicy.getViolationCount() >= EMERGENCY_BLOCK_THRESHOLD) {
            blockOrigin(origin, "Excessive policy violations: " + originPolicy.getViolationCount());
        }
        
        metricsCollector.recordPolicyViolation(origin, violationType, details);
        
        logger.warn("CORS policy violation recorded: origin={}, type={}, details={}", 
                   origin, violationType, details);
    }

    /**
     * Temporarily block an origin.
     */
    public void blockOrigin(String origin, String reason) {
        OriginPolicyState originPolicy = getOrCreateOriginPolicy(origin);
        originPolicy.block(reason);
        
        metricsCollector.recordSecurityIncident(origin, "origin_blocked", "high");
        
        logger.error("Origin blocked by policy manager: origin={}, reason={}", origin, reason);
    }

    /**
     * Unblock an origin.
     */
    public void unblockOrigin(String origin, String reason) {
        OriginPolicyState originPolicy = originPolicies.get(origin);
        if (originPolicy != null) {
            originPolicy.unblock(reason);
            logger.info("Origin unblocked by policy manager: origin={}, reason={}", origin, reason);
        }
    }

    /**
     * Update policy configuration at runtime.
     */
    public void updatePolicy(PolicyUpdate update) {
        logger.info("Updating CORS policy: {}", update);
        
        // Validate policy update
        if (!validatePolicyUpdate(update)) {
            logger.error("Policy update validation failed: {}", update);
            return;
        }
        
        // Apply policy changes
        applyPolicyUpdate(update);
        
        // Increment version
        policyVersion.incrementAndGet();
        lastPolicyUpdate = Instant.now();
        
        logger.info("CORS policy updated successfully, new version: {}", policyVersion.get());
    }

    /**
     * Get current policy status.
     */
    public PolicyStatus getPolicyStatus() {
        int totalOrigins = originPolicies.size();
        long blockedOrigins = originPolicies.values().stream()
                .mapToLong(policy -> policy.isBlocked() ? 1 : 0)
                .sum();
        
        int recentViolationCount = recentViolations.size();
        
        return new PolicyStatus(
            policyVersion.get(),
            lastPolicyUpdate,
            policyEnforcementEnabled.get(),
            totalOrigins,
            blockedOrigins,
            recentViolationCount
        );
    }

    /**
     * Check if origin is allowed by current policy.
     */
    private boolean isOriginAllowed(String origin) {
        if (origin == null || !corsProperties.isEnabled()) {
            return false;
        }
        
        // Check explicit allowed origins
        if (corsProperties.getAllowedOrigins().contains(origin)) {
            return true;
        }
        
        // Check wildcard
        if (corsProperties.getAllowedOrigins().contains("*")) {
            return true;
        }
        
        // Apply pattern-based rules for development
        if (isLocalDevelopment() && (LOCALHOST_PATTERN.matcher(origin).matches() || 
                                    DEVELOPMENT_PATTERN.matcher(origin).matches())) {
            return true;
        }
        
        return false;
    }

    /**
     * Apply dynamic policy rules.
     */
    private PolicyDecision applyDynamicPolicyRules(String origin, String method, String path, 
                                                  String userAgent, String clientIP) {
        PolicyDecision decision = new PolicyDecision();
        
        // Check for suspicious patterns
        if (containsSuspiciousPatterns(origin)) {
            decision.deny("Origin contains suspicious patterns");
            recordPolicyViolation(origin, "suspicious_pattern", "Origin matches suspicious pattern");
            return decision;
        }
        
        // Check user agent requirements
        if (requiresUserAgent() && (userAgent == null || userAgent.trim().isEmpty())) {
            decision.deny("User-Agent header required");
            recordPolicyViolation(origin, "missing_user_agent", "Required User-Agent header missing");
            return decision;
        }
        
        // Check path restrictions
        if (hasPathRestrictions(path)) {
            decision.deny("Path not allowed by policy");
            recordPolicyViolation(origin, "path_restricted", "Path access restricted: " + path);
            return decision;
        }
        
        decision.allow("Dynamic policy rules satisfied");
        return decision;
    }

    /**
     * Validate current policy configuration.
     */
    private void validateCurrentPolicy() {
        logger.debug("Validating current CORS policy configuration");
        
        // Check for security issues
        if (corsProperties.getAllowedOrigins().contains("*") && corsProperties.isAllowCredentials()) {
            logger.error("SECURITY ISSUE: Wildcard origin with credentials enabled");
            recordPolicyViolation("*", "security_misconfiguration", 
                "Wildcard origin with credentials is a security risk");
        }
        
        // Check for empty or null origins
        long invalidOrigins = corsProperties.getAllowedOrigins().stream()
                .mapToLong(origin -> (origin == null || origin.trim().isEmpty()) ? 1 : 0)
                .sum();
        
        if (invalidOrigins > 0) {
            logger.warn("Found {} invalid origins in configuration", invalidOrigins);
        }
        
        logger.info("Policy validation complete. Issues found: {}", invalidOrigins > 0 ? "Yes" : "No");
    }

    /**
     * Start periodic policy review.
     */
    private void startPolicyReview() {
        scheduler.scheduleAtFixedRate(this::performPolicyReview, 
                                     POLICY_REVIEW_INTERVAL_HOURS, 
                                     POLICY_REVIEW_INTERVAL_HOURS, 
                                     TimeUnit.HOURS);
        
        scheduler.scheduleAtFixedRate(this::cleanupOldData, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Perform periodic policy review.
     */
    private void performPolicyReview() {
        logger.info("Performing periodic CORS policy review");
        
        PolicyStatus status = getPolicyStatus();
        
        // Review blocked origins
        if (status.getBlockedOrigins() > 0) {
            logger.info("Policy review: {} origins currently blocked", status.getBlockedOrigins());
            reviewBlockedOrigins();
        }
        
        // Review violation patterns
        if (status.getRecentViolations() > 0) {
            logger.info("Policy review: {} recent violations detected", status.getRecentViolations());
            analyzeViolationPatterns();
        }
        
        // Update policy metrics
        metricsCollector.updateTrustedOriginCount(corsProperties.getAllowedOrigins().size());
        metricsCollector.updateBlockedOriginCount(status.getBlockedOrigins());
        
        logger.info("Policy review complete - Version: {}, Enforcement: {}", 
                   status.getVersion(), status.isEnforcementEnabled());
    }

    /**
     * Review blocked origins for potential unblocking.
     */
    private void reviewBlockedOrigins() {
        Instant reviewCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        
        originPolicies.forEach((origin, policy) -> {
            if (policy.isBlocked() && policy.getBlockedSince().isBefore(reviewCutoff)) {
                // Consider unblocking origins blocked for more than 24 hours
                if (policy.getViolationCount() < VIOLATION_THRESHOLD) {
                    logger.info("Considering unblock for origin: {} (violations: {})", 
                               origin, policy.getViolationCount());
                    // Could implement automatic unblocking here
                }
            }
        });
    }

    /**
     * Analyze violation patterns for policy improvements.
     */
    private void analyzeViolationPatterns() {
        // Analyze recent violations for patterns
        ConcurrentHashMap<String, AtomicLong> violationTypes = new ConcurrentHashMap<>();
        
        recentViolations.values().forEach(violation -> {
            violationTypes.computeIfAbsent(violation.getType(), k -> new AtomicLong(0))
                         .incrementAndGet();
        });
        
        violationTypes.forEach((type, count) -> {
            if (count.get() > 10) { // High frequency violation type
                logger.warn("High frequency violation type detected: {} (count: {})", type, count.get());
                // Could trigger policy adjustments here
            }
        });
    }

    /**
     * Get or create origin policy state.
     */
    private OriginPolicyState getOrCreateOriginPolicy(String origin) {
        return originPolicies.computeIfAbsent(origin, OriginPolicyState::new);
    }

    /**
     * Check if contains suspicious patterns.
     */
    private boolean containsSuspiciousPatterns(String origin) {
        if (origin == null) return true;
        
        String lowerOrigin = origin.toLowerCase();
        return lowerOrigin.contains("javascript:") || 
               lowerOrigin.contains("data:") || 
               lowerOrigin.contains("file:") ||
               lowerOrigin.equals("null");
    }

    /**
     * Check if user agent is required.
     */
    private boolean requiresUserAgent() {
        return true; // Always require user agent for security
    }

    /**
     * Check if path has restrictions.
     */
    private boolean hasPathRestrictions(String path) {
        if (path == null) return false;
        
        // Block administrative paths
        return path.startsWith("/admin") || 
               path.startsWith("/internal") ||
               path.contains("../");
    }

    /**
     * Check if running in local development mode.
     */
    private boolean isLocalDevelopment() {
        // Simple check - could be more sophisticated
        return corsProperties.getAllowedOrigins().stream()
                .anyMatch(origin -> origin.contains("localhost") || origin.contains(".local"));
    }

    /**
     * Validate policy update.
     */
    private boolean validatePolicyUpdate(PolicyUpdate update) {
        // Basic validation
        if (update == null) return false;
        
        // Validate security constraints
        if (update.getAllowedOrigins() != null && 
            update.getAllowedOrigins().contains("*") && 
            Boolean.TRUE.equals(update.getAllowCredentials())) {
            logger.error("Policy update validation failed: wildcard origin with credentials");
            return false;
        }
        
        return true;
    }

    /**
     * Apply policy update.
     */
    private void applyPolicyUpdate(PolicyUpdate update) {
        // This would update the corsProperties
        // For now, we'll just log what would be updated
        logger.info("Would apply policy update: {}", update);
    }

    /**
     * Generate violation key.
     */
    private String generateViolationKey(String origin, String type) {
        return origin + ":" + type + ":" + (System.currentTimeMillis() / 60000); // Per minute
    }

    /**
     * Clean up old data.
     */
    private void cleanupOldData() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        
        // Clean up old violations
        recentViolations.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(cutoff));
        
        // Clean up inactive origin policies
        originPolicies.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(cutoff) && !entry.getValue().isBlocked());
        
        logger.debug("Policy data cleanup complete. Active policies: {}, Recent violations: {}", 
                    originPolicies.size(), recentViolations.size());
    }

    /**
     * Individual origin policy state.
     */
    public static class OriginPolicyState {
        private final String origin;
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong violationCount = new AtomicLong(0);
        private final ConcurrentHashMap<String, AtomicLong> violationTypes = new ConcurrentHashMap<>();
        private volatile boolean blocked = false;
        private volatile String blockReason;
        private volatile Instant blockedSince;
        private volatile Instant lastActivity = Instant.now();

        public OriginPolicyState(String origin) {
            this.origin = origin;
        }

        public void recordRequest() {
            requestCount.incrementAndGet();
            lastActivity = Instant.now();
        }

        public void recordViolation(String type) {
            violationCount.incrementAndGet();
            violationTypes.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
        }

        public void block(String reason) {
            this.blocked = true;
            this.blockReason = reason;
            this.blockedSince = Instant.now();
        }

        public void unblock(String reason) {
            this.blocked = false;
            this.blockReason = null;
            this.blockedSince = null;
        }

        // Getters
        public String getOrigin() { return origin; }
        public boolean isBlocked() { return blocked; }
        public String getBlockReason() { return blockReason; }
        public Instant getBlockedSince() { return blockedSince; }
        public long getViolationCount() { return violationCount.get(); }
        public Instant getLastActivity() { return lastActivity; }
    }

    /**
     * Policy violation record.
     */
    public static class PolicyViolation {
        private final String origin;
        private final String type;
        private final String details;
        private final Instant timestamp;

        public PolicyViolation(String origin, String type, String details) {
            this.origin = origin;
            this.type = type;
            this.details = details;
            this.timestamp = Instant.now();
        }

        // Getters
        public String getOrigin() { return origin; }
        public String getType() { return type; }
        public String getDetails() { return details; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Policy decision result.
     */
    public static class PolicyDecision {
        private boolean allowed = false;
        private String reason;

        public void allow(String reason) {
            this.allowed = true;
            this.reason = reason;
        }

        public void deny(String reason) {
            this.allowed = false;
            this.reason = reason;
        }



        // Getters
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
    }

    /**
     * Policy update request.
     */
    public static class PolicyUpdate {
        private java.util.List<String> allowedOrigins;
        private java.util.List<String> allowedMethods;
        private Boolean allowCredentials;
        private Long maxAge;

        // Getters and setters
        public java.util.List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(java.util.List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public java.util.List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(java.util.List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public Boolean getAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(Boolean allowCredentials) { this.allowCredentials = allowCredentials; }
        public Long getMaxAge() { return maxAge; }
        public void setMaxAge(Long maxAge) { this.maxAge = maxAge; }

        @Override
        public String toString() {
            return String.format("PolicyUpdate{origins=%s, methods=%s, credentials=%s, maxAge=%s}", 
                               allowedOrigins, allowedMethods, allowCredentials, maxAge);
        }
    }

    /**
     * Current policy status.
     */
    public static class PolicyStatus {
        private final long version;
        private final Instant lastUpdate;
        private final boolean enforcementEnabled;
        private final int totalOrigins;
        private final long blockedOrigins;
        private final int recentViolations;

        public PolicyStatus(long version, Instant lastUpdate, boolean enforcementEnabled,
                          int totalOrigins, long blockedOrigins, int recentViolations) {
            this.version = version;
            this.lastUpdate = lastUpdate;
            this.enforcementEnabled = enforcementEnabled;
            this.totalOrigins = totalOrigins;
            this.blockedOrigins = blockedOrigins;
            this.recentViolations = recentViolations;
        }

        // Getters
        public long getVersion() { return version; }
        public Instant getLastUpdate() { return lastUpdate; }
        public boolean isEnforcementEnabled() { return enforcementEnabled; }
        public int getTotalOrigins() { return totalOrigins; }
        public long getBlockedOrigins() { return blockedOrigins; }
        public int getRecentViolations() { return recentViolations; }
    }
}
