package com.mambogo.gateway.security;

import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CORS security alert management and escalation.
 * 
 * Handles CORS security violation events, manages alert thresholds,
 * implements alert suppression, and coordinates incident response.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsAlertManager {

    private static final Logger logger = LoggerFactory.getLogger(CorsAlertManager.class);
    
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    // Alert tracking
    private final ConcurrentHashMap<String, AlertTracker> alertTrackers = new ConcurrentHashMap<>();
    private final AtomicLong totalAlertsGenerated = new AtomicLong(0);
    private final AtomicLong totalIncidentsDetected = new AtomicLong(0);
    
    // Alert thresholds
    private static final int CRITICAL_ALERT_THRESHOLD = 10; // alerts per 5 minutes
    private static final int HIGH_ALERT_THRESHOLD = 25; // alerts per 15 minutes
    private static final int ALERT_SUPPRESSION_MINUTES = 10;
    private static final int INCIDENT_ESCALATION_THRESHOLD = 5; // critical alerts trigger incident

    public CorsAlertManager(SimpleCorsMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Start periodic alert processing
        startAlertProcessing();
        
        logger.info("CORS Alert Manager initialized with thresholds: critical={}/5min, high={}/15min", 
                   CRITICAL_ALERT_THRESHOLD, HIGH_ALERT_THRESHOLD);
    }

    /**
     * Handle CORS violation events.
     */
    @EventListener
    public void handleCorsViolation(CorsViolationEvent event) {
        String origin = event.getOrigin();
        String severity = event.getSeverity();
        
        AlertTracker tracker = alertTrackers.computeIfAbsent(origin, AlertTracker::new);
        
        // Check if alert should be suppressed
        if (tracker.shouldSuppressAlert(severity)) {
            logger.debug("Alert suppressed for origin: {} (severity: {})", origin, severity);
            return;
        }
        
        // Generate alert
        Alert alert = new Alert(
            generateAlertId(),
            origin,
            severity,
            event.getViolations(),
            event.getMethod(),
            event.getPath(),
            event.getClientIP(),
            event.getUserAgent()
        );
        
        // Process alert
        processAlert(alert);
        
        // Update tracking
        tracker.recordAlert(severity);
        totalAlertsGenerated.incrementAndGet();
        
        // Record metrics
        metricsCollector.recordSecurityIncident(origin, "alert_generated", severity);
        
        logger.info("CORS security alert generated: {}", alert);
    }

    /**
     * Process an individual alert.
     */
    private void processAlert(Alert alert) {
        String severity = alert.getSeverity();
        String origin = alert.getOrigin();
        
        switch (severity.toLowerCase()) {
            case "critical":
                handleCriticalAlert(alert);
                break;
            case "high":
                handleHighAlert(alert);
                break;
            case "medium":
                handleMediumAlert(alert);
                break;
            case "low":
                handleLowAlert(alert);
                break;
            default:
                logger.warn("Unknown alert severity: {}", severity);
        }
        
        // Check for incident escalation
        checkIncidentEscalation(origin);
    }

    /**
     * Handle critical severity alerts.
     */
    private void handleCriticalAlert(Alert alert) {
        logger.error("CRITICAL CORS ALERT: Origin={}, Violations={}, IP={}", 
                    alert.getOrigin(), alert.getViolationSummary(), alert.getClientIP());
        
        // Immediate notification actions would go here
        // e.g., PagerDuty, Slack, email notifications
        
        // Consider automatic blocking for critical violations
        if (shouldAutoBlock(alert)) {
            initiateAutoBlock(alert.getOrigin(), "Critical security violations");
        }
    }

    /**
     * Handle high severity alerts.
     */
    private void handleHighAlert(Alert alert) {
        logger.warn("HIGH CORS ALERT: Origin={}, Violations={}, IP={}", 
                   alert.getOrigin(), alert.getViolationSummary(), alert.getClientIP());
        
        // High priority notification actions
        // e.g., team notifications, dashboard updates
    }

    /**
     * Handle medium severity alerts.
     */
    private void handleMediumAlert(Alert alert) {
        logger.warn("MEDIUM CORS ALERT: Origin={}, Violations={}", 
                   alert.getOrigin(), alert.getViolationSummary());
        
        // Standard notification actions
    }

    /**
     * Handle low severity alerts.
     */
    private void handleLowAlert(Alert alert) {
        logger.info("LOW CORS ALERT: Origin={}, Violations={}", 
                   alert.getOrigin(), alert.getViolationSummary());
        
        // Informational logging
    }

    /**
     * Check if incident escalation is needed.
     */
    private void checkIncidentEscalation(String origin) {
        AlertTracker tracker = alertTrackers.get(origin);
        if (tracker != null && tracker.getCriticalAlertsInLast5Minutes() >= INCIDENT_ESCALATION_THRESHOLD) {
            escalateToIncident(origin, tracker);
        }
    }

    /**
     * Escalate to security incident.
     */
    private void escalateToIncident(String origin, AlertTracker tracker) {
        logger.error("SECURITY INCIDENT: Origin {} has {} critical alerts in 5 minutes - escalating to incident", 
                    origin, tracker.getCriticalAlertsInLast5Minutes());
        
        totalIncidentsDetected.incrementAndGet();
        metricsCollector.recordSecurityIncident(origin, "escalated_incident", "critical");
        
        // Incident response actions
        // e.g., automatic blocking, incident ticket creation, team notification
        initiateIncidentResponse(origin);
    }

    /**
     * Determine if automatic blocking should be applied.
     */
    private boolean shouldAutoBlock(Alert alert) {
        // Auto-block criteria
        if ("critical".equals(alert.getSeverity())) {
            // Check for specific violation types that warrant auto-blocking
            for (SecurityViolation violation : alert.getViolations()) {
                if ("potential_xss".equals(violation.getType()) || 
                    "sql_injection_attempt".equals(violation.getType()) ||
                    "path_traversal".equals(violation.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initiate automatic blocking for an origin.
     */
    private void initiateAutoBlock(String origin, String reason) {
        logger.error("INITIATING AUTO-BLOCK: Origin={}, Reason={}", origin, reason);
        
        // Implementation would integrate with actual blocking mechanism
        // e.g., update firewall rules, add to blacklist, etc.
        
        metricsCollector.recordSecurityIncident(origin, "auto_blocked", "critical");
    }

    /**
     * Initiate incident response procedures.
     */
    private void initiateIncidentResponse(String origin) {
        logger.error("INITIATING INCIDENT RESPONSE: Origin={}", origin);
        
        // Incident response actions:
        // 1. Create incident ticket
        // 2. Notify security team
        // 3. Gather forensic data
        // 4. Apply containment measures
        
        // For now, we'll log and track the incident
        metricsCollector.recordSecurityIncident(origin, "incident_response", "critical");
    }

    /**
     * Start periodic alert processing.
     */
    private void startAlertProcessing() {
        // Process alert statistics every 5 minutes
        scheduler.scheduleAtFixedRate(this::processAlertStatistics, 5, 5, TimeUnit.MINUTES);
        
        // Cleanup old alert data every hour
        scheduler.scheduleAtFixedRate(this::cleanupOldAlerts, 60, 60, TimeUnit.MINUTES);
    }

    /**
     * Process alert statistics and trends.
     */
    private void processAlertStatistics() {
        logger.debug("Processing CORS alert statistics");
        
        int activeOrigins = alertTrackers.size();
        long totalAlerts = totalAlertsGenerated.get();
        long totalIncidents = totalIncidentsDetected.get();
        
        // Analyze alert trends
        long highActivityOrigins = alertTrackers.values().stream()
                .mapToLong(tracker -> tracker.getAlertsInLast15Minutes() > HIGH_ALERT_THRESHOLD ? 1 : 0)
                .sum();
        
        if (highActivityOrigins > 0) {
            logger.warn("High alert activity detected: {} origins exceeding threshold", highActivityOrigins);
        }
        
        logger.info("Alert Statistics: Total={}, Incidents={}, Active Origins={}, High Activity={}", 
                   totalAlerts, totalIncidents, activeOrigins, highActivityOrigins);
    }

    /**
     * Clean up old alert data.
     */
    private void cleanupOldAlerts() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        
        alertTrackers.values().forEach(tracker -> tracker.cleanup(cutoff));
        alertTrackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        logger.debug("Alert data cleanup complete. Active trackers: {}", alertTrackers.size());
    }

    /**
     * Generate unique alert ID.
     */
    private String generateAlertId() {
        return "CORS-" + System.currentTimeMillis() + "-" + (totalAlertsGenerated.get() % 1000);
    }

    /**
     * Get alert statistics.
     */
    public AlertStatistics getAlertStatistics() {
        int activeOrigins = alertTrackers.size();
        long totalAlerts = totalAlertsGenerated.get();
        long totalIncidents = totalIncidentsDetected.get();
        
        long criticalAlertsLast5Min = alertTrackers.values().stream()
                .mapToLong(AlertTracker::getCriticalAlertsInLast5Minutes)
                .sum();
        
        long highAlertsLast15Min = alertTrackers.values().stream()
                .mapToLong(AlertTracker::getAlertsInLast15Minutes)
                .sum();
        
        return new AlertStatistics(totalAlerts, totalIncidents, activeOrigins, 
                                 criticalAlertsLast5Min, highAlertsLast15Min);
    }

    /**
     * Alert tracking per origin.
     */
    private static class AlertTracker {
        private final AtomicInteger totalAlerts = new AtomicInteger(0);
        private final AtomicInteger criticalAlerts = new AtomicInteger(0);
        private final ConcurrentHashMap<String, AtomicInteger> alertsBySeverity = new ConcurrentHashMap<>();
        private volatile Instant lastAlert = Instant.now();
        private volatile Instant lastCriticalAlert = null;

        public AlertTracker(String origin) {
        }

        public boolean shouldSuppressAlert(String severity) {
            // Suppress if same severity alert was generated recently
            Instant suppressionCutoff = Instant.now().minus(ALERT_SUPPRESSION_MINUTES, ChronoUnit.MINUTES);
            
            if ("critical".equals(severity) && lastCriticalAlert != null) {
                return lastCriticalAlert.isAfter(suppressionCutoff);
            }
            
            // General suppression for non-critical alerts
            return lastAlert.isAfter(suppressionCutoff) && !"critical".equals(severity);
        }

        public void recordAlert(String severity) {
            totalAlerts.incrementAndGet();
            alertsBySeverity.computeIfAbsent(severity, k -> new AtomicInteger(0)).incrementAndGet();
            lastAlert = Instant.now();
            
            if ("critical".equals(severity)) {
                criticalAlerts.incrementAndGet();
                lastCriticalAlert = Instant.now();
            }
        }

        public int getCriticalAlertsInLast5Minutes() {
            // Simplified implementation - would need time-based tracking for accuracy
            return Math.min(criticalAlerts.get(), CRITICAL_ALERT_THRESHOLD);
        }

        public int getAlertsInLast15Minutes() {
            // Simplified implementation - would need time-based tracking for accuracy
            return Math.min(totalAlerts.get(), HIGH_ALERT_THRESHOLD);
        }

        public void cleanup(Instant cutoff) {
            // Cleanup logic for time-based data
        }

        public boolean isEmpty() {
            return totalAlerts.get() == 0;
        }
    }

    /**
     * Individual security alert.
     */
    public static class Alert {
        private final String alertId;
        private final String origin;
        private final String severity;
        private final java.util.List<SecurityViolation> violations;
        private final String method;
        private final String path;
        private final String clientIP;
        private final String userAgent;
        private final Instant timestamp;

        public Alert(String alertId, String origin, String severity, java.util.List<SecurityViolation> violations,
                    String method, String path, String clientIP, String userAgent) {
            this.alertId = alertId;
            this.origin = origin;
            this.severity = severity;
            this.violations = violations;
            this.method = method;
            this.path = path;
            this.clientIP = clientIP;
            this.userAgent = userAgent;
            this.timestamp = Instant.now();
        }

        public String getViolationSummary() {
            return violations.stream()
                    .map(SecurityViolation::getType)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("unknown");
        }

        // Getters
        public String getAlertId() { return alertId; }
        public String getOrigin() { return origin; }
        public String getSeverity() { return severity; }
        public java.util.List<SecurityViolation> getViolations() { return violations; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getClientIP() { return clientIP; }
        public String getUserAgent() { return userAgent; }
        public Instant getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("Alert{id='%s', origin='%s', severity='%s', violations='%s'}", 
                               alertId, origin, severity, getViolationSummary());
        }
    }

    /**
     * Alert statistics summary.
     */
    public static class AlertStatistics {
        private final long totalAlerts;
        private final long totalIncidents;
        private final int activeOrigins;
        private final long criticalAlertsLast5Min;
        private final long highAlertsLast15Min;

        public AlertStatistics(long totalAlerts, long totalIncidents, int activeOrigins,
                             long criticalAlertsLast5Min, long highAlertsLast15Min) {
            this.totalAlerts = totalAlerts;
            this.totalIncidents = totalIncidents;
            this.activeOrigins = activeOrigins;
            this.criticalAlertsLast5Min = criticalAlertsLast5Min;
            this.highAlertsLast15Min = highAlertsLast15Min;
        }

        // Getters
        public long getTotalAlerts() { return totalAlerts; }
        public long getTotalIncidents() { return totalIncidents; }
        public int getActiveOrigins() { return activeOrigins; }
        public long getCriticalAlertsLast5Min() { return criticalAlertsLast5Min; }
        public long getHighAlertsLast15Min() { return highAlertsLast15Min; }
    }
}
