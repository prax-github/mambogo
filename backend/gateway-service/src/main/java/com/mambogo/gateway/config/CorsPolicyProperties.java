package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.List;

/**
 * Advanced CORS policy configuration properties.
 * 
 * Extends basic CORS configuration with policy management,
 * security settings, and compliance features.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@ConfigurationProperties(prefix = "mambogo.cors.policy")
public class CorsPolicyProperties {

    /**
     * Whether policy enforcement is enabled.
     */
    private boolean enforcementEnabled = true;

    /**
     * Whether to automatically block origins with excessive violations.
     */
    private boolean autoBlockEnabled = true;

    /**
     * Threshold for automatic blocking (number of violations).
     */
    private int autoBlockThreshold = 10;

    /**
     * Duration in hours for automatic blocks.
     */
    private int autoBlockDurationHours = 24;

    /**
     * Whether to require User-Agent header.
     */
    private boolean requireUserAgent = true;

    /**
     * Maximum allowed request rate per origin (requests per minute).
     */
    private int maxRequestsPerMinute = 100;

    /**
     * Whether to enable compliance validation.
     */
    private boolean complianceValidationEnabled = true;

    /**
     * Compliance check interval in hours.
     */
    private int complianceCheckIntervalHours = 24;

    /**
     * Whether to enable audit logging.
     */
    private boolean auditLoggingEnabled = true;

    /**
     * Audit log batch size.
     */
    private int auditLogBatchSize = 100;

    /**
     * Audit log flush interval in seconds.
     */
    private int auditLogFlushIntervalSeconds = 30;

    /**
     * Whether to enable security monitoring.
     */
    private boolean securityMonitoringEnabled = true;

    /**
     * Security analysis interval in minutes.
     */
    private int securityAnalysisIntervalMinutes = 5;

    /**
     * Whether to enable performance monitoring.
     */
    private boolean performanceMonitoringEnabled = true;

    /**
     * Performance warning threshold in milliseconds.
     */
    private long performanceWarningThresholdMs = 50;

    /**
     * Performance critical threshold in milliseconds.
     */
    private long performanceCriticalThresholdMs = 100;

    /**
     * List of origins that should never be blocked.
     */
    private List<String> trustedOrigins = List.of();

    /**
     * List of origins that are permanently blocked.
     */
    private List<String> blockedOrigins = List.of();

    /**
     * List of suspicious origin patterns to monitor.
     */
    private List<String> suspiciousPatterns = List.of(
        ".*javascript:.*",
        ".*data:.*",
        ".*file:.*",
        "null",
        "undefined"
    );

    /**
     * Whether to enable alert notifications.
     */
    private boolean alertNotificationsEnabled = true;

    /**
     * Alert suppression interval in minutes.
     */
    private int alertSuppressionIntervalMinutes = 10;

    /**
     * Incident escalation threshold (critical alerts).
     */
    private int incidentEscalationThreshold = 5;

    // Constructors
    public CorsPolicyProperties() {}

    // Getters and Setters
    public boolean isEnforcementEnabled() {
        return enforcementEnabled;
    }

    public void setEnforcementEnabled(boolean enforcementEnabled) {
        this.enforcementEnabled = enforcementEnabled;
    }

    public boolean isAutoBlockEnabled() {
        return autoBlockEnabled;
    }

    public void setAutoBlockEnabled(boolean autoBlockEnabled) {
        this.autoBlockEnabled = autoBlockEnabled;
    }

    public int getAutoBlockThreshold() {
        return autoBlockThreshold;
    }

    public void setAutoBlockThreshold(int autoBlockThreshold) {
        this.autoBlockThreshold = autoBlockThreshold;
    }

    public int getAutoBlockDurationHours() {
        return autoBlockDurationHours;
    }

    public void setAutoBlockDurationHours(int autoBlockDurationHours) {
        this.autoBlockDurationHours = autoBlockDurationHours;
    }

    public boolean isRequireUserAgent() {
        return requireUserAgent;
    }

    public void setRequireUserAgent(boolean requireUserAgent) {
        this.requireUserAgent = requireUserAgent;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public boolean isComplianceValidationEnabled() {
        return complianceValidationEnabled;
    }

    public void setComplianceValidationEnabled(boolean complianceValidationEnabled) {
        this.complianceValidationEnabled = complianceValidationEnabled;
    }

    public int getComplianceCheckIntervalHours() {
        return complianceCheckIntervalHours;
    }

    public void setComplianceCheckIntervalHours(int complianceCheckIntervalHours) {
        this.complianceCheckIntervalHours = complianceCheckIntervalHours;
    }

    public boolean isAuditLoggingEnabled() {
        return auditLoggingEnabled;
    }

    public void setAuditLoggingEnabled(boolean auditLoggingEnabled) {
        this.auditLoggingEnabled = auditLoggingEnabled;
    }

    public int getAuditLogBatchSize() {
        return auditLogBatchSize;
    }

    public void setAuditLogBatchSize(int auditLogBatchSize) {
        this.auditLogBatchSize = auditLogBatchSize;
    }

    public int getAuditLogFlushIntervalSeconds() {
        return auditLogFlushIntervalSeconds;
    }

    public void setAuditLogFlushIntervalSeconds(int auditLogFlushIntervalSeconds) {
        this.auditLogFlushIntervalSeconds = auditLogFlushIntervalSeconds;
    }

    public boolean isSecurityMonitoringEnabled() {
        return securityMonitoringEnabled;
    }

    public void setSecurityMonitoringEnabled(boolean securityMonitoringEnabled) {
        this.securityMonitoringEnabled = securityMonitoringEnabled;
    }

    public int getSecurityAnalysisIntervalMinutes() {
        return securityAnalysisIntervalMinutes;
    }

    public void setSecurityAnalysisIntervalMinutes(int securityAnalysisIntervalMinutes) {
        this.securityAnalysisIntervalMinutes = securityAnalysisIntervalMinutes;
    }

    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }

    public void setPerformanceMonitoringEnabled(boolean performanceMonitoringEnabled) {
        this.performanceMonitoringEnabled = performanceMonitoringEnabled;
    }

    public long getPerformanceWarningThresholdMs() {
        return performanceWarningThresholdMs;
    }

    public void setPerformanceWarningThresholdMs(long performanceWarningThresholdMs) {
        this.performanceWarningThresholdMs = performanceWarningThresholdMs;
    }

    public long getPerformanceCriticalThresholdMs() {
        return performanceCriticalThresholdMs;
    }

    public void setPerformanceCriticalThresholdMs(long performanceCriticalThresholdMs) {
        this.performanceCriticalThresholdMs = performanceCriticalThresholdMs;
    }

    public List<String> getTrustedOrigins() {
        return trustedOrigins;
    }

    public void setTrustedOrigins(List<String> trustedOrigins) {
        this.trustedOrigins = trustedOrigins;
    }

    public List<String> getBlockedOrigins() {
        return blockedOrigins;
    }

    public void setBlockedOrigins(List<String> blockedOrigins) {
        this.blockedOrigins = blockedOrigins;
    }

    public List<String> getSuspiciousPatterns() {
        return suspiciousPatterns;
    }

    public void setSuspiciousPatterns(List<String> suspiciousPatterns) {
        this.suspiciousPatterns = suspiciousPatterns;
    }

    public boolean isAlertNotificationsEnabled() {
        return alertNotificationsEnabled;
    }

    public void setAlertNotificationsEnabled(boolean alertNotificationsEnabled) {
        this.alertNotificationsEnabled = alertNotificationsEnabled;
    }

    public int getAlertSuppressionIntervalMinutes() {
        return alertSuppressionIntervalMinutes;
    }

    public void setAlertSuppressionIntervalMinutes(int alertSuppressionIntervalMinutes) {
        this.alertSuppressionIntervalMinutes = alertSuppressionIntervalMinutes;
    }

    public int getIncidentEscalationThreshold() {
        return incidentEscalationThreshold;
    }

    public void setIncidentEscalationThreshold(int incidentEscalationThreshold) {
        this.incidentEscalationThreshold = incidentEscalationThreshold;
    }

    @Override
    public String toString() {
        return "CorsPolicyProperties{" +
                "enforcementEnabled=" + enforcementEnabled +
                ", autoBlockEnabled=" + autoBlockEnabled +
                ", autoBlockThreshold=" + autoBlockThreshold +
                ", maxRequestsPerMinute=" + maxRequestsPerMinute +
                ", complianceValidationEnabled=" + complianceValidationEnabled +
                ", securityMonitoringEnabled=" + securityMonitoringEnabled +
                ", performanceMonitoringEnabled=" + performanceMonitoringEnabled +
                ", trustedOrigins=" + trustedOrigins.size() +
                ", blockedOrigins=" + blockedOrigins.size() +
                '}';
    }
}
