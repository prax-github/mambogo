package com.mambogo.gateway.policy;

import com.mambogo.gateway.config.CorsProperties;
import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CORS compliance validation and audit capabilities.
 * 
 * Validates CORS configuration against security best practices,
 * regulatory requirements, and industry standards.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsComplianceValidator {

    private static final Logger logger = LoggerFactory.getLogger(CorsComplianceValidator.class);
    
    private final CorsProperties corsProperties;
    private final SimpleCorsMetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    // Compliance tracking
    private volatile ComplianceReport lastComplianceReport;
    private volatile Instant lastComplianceCheck = Instant.now();

    public CorsComplianceValidator(CorsProperties corsProperties, SimpleCorsMetricsCollector metricsCollector) {
        this.corsProperties = corsProperties;
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        logger.info("CORS Compliance Validator initialized");
    }

    /**
     * Initialize compliance validation after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeComplianceValidation() {
        logger.info("Starting CORS compliance validation");
        
        // Perform initial compliance check
        performComplianceCheck();
        
        // Schedule periodic compliance checks
        scheduler.scheduleAtFixedRate(this::performComplianceCheck, 24, 24, TimeUnit.HOURS);
        
        logger.info("CORS compliance validation initialized");
    }

    /**
     * Perform comprehensive compliance check.
     */
    public ComplianceReport performComplianceCheck() {
        logger.info("Performing CORS compliance validation");
        
        List<ComplianceIssue> issues = new ArrayList<>();
        
        // Check security best practices
        validateSecurityBestPractices(issues);
        
        // Check OWASP guidelines
        validateOWASPGuidelines(issues);
        
        // Check configuration consistency
        validateConfigurationConsistency(issues);
        
        // Check performance recommendations
        validatePerformanceRecommendations(issues);
        
        // Check operational requirements
        validateOperationalRequirements(issues);
        
        // Generate compliance report
        ComplianceReport report = new ComplianceReport(issues);
        lastComplianceReport = report;
        lastComplianceCheck = Instant.now();
        
        // Log compliance summary
        logComplianceSummary(report);
        
        // Record metrics
        recordComplianceMetrics(report);
        
        return report;
    }

    /**
     * Get the latest compliance report.
     */
    public ComplianceReport getLatestComplianceReport() {
        if (lastComplianceReport == null) {
            return performComplianceCheck();
        }
        return lastComplianceReport;
    }

    /**
     * Check if current configuration is compliant.
     */
    public boolean isCompliant() {
        ComplianceReport report = getLatestComplianceReport();
        return report.getCriticalIssues().isEmpty() && report.getHighIssues().isEmpty();
    }

    /**
     * Get compliance score (0-100).
     */
    public int getComplianceScore() {
        ComplianceReport report = getLatestComplianceReport();
        
        int totalIssues = report.getIssues().size();
        if (totalIssues == 0) return 100;
        
        // Weight issues by severity
        int weightedIssues = 0;
        for (ComplianceIssue issue : report.getIssues()) {
            switch (issue.getSeverity()) {
                case CRITICAL: weightedIssues += 10; break;
                case HIGH: weightedIssues += 5; break;
                case MEDIUM: weightedIssues += 2; break;
                case LOW: weightedIssues += 1; break;
            }
        }
        
        // Calculate score (max 100 weighted issues for 0 score)
        return Math.max(0, 100 - weightedIssues);
    }

    /**
     * Validate security best practices.
     */
    private void validateSecurityBestPractices(List<ComplianceIssue> issues) {
        // Check for wildcard origin with credentials
        if (corsProperties.getAllowedOrigins().contains("*") && corsProperties.isAllowCredentials()) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.CRITICAL,
                "SECURITY_WILDCARD_CREDENTIALS",
                "Wildcard origin (*) with allowCredentials=true is a critical security vulnerability",
                "Remove wildcard origin or set allowCredentials=false",
                "OWASP CORS Security Guidelines"
            ));
        }
        
        // Check for overly permissive origins
        long httpOrigins = corsProperties.getAllowedOrigins().stream()
                .filter(origin -> origin.startsWith("http://"))
                .count();
        
        if (httpOrigins > 0) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.HIGH,
                "SECURITY_HTTP_ORIGINS",
                "HTTP origins are not secure and should use HTTPS",
                "Update all origins to use HTTPS protocol",
                "Transport Layer Security Best Practices"
            ));
        }
        
        // Check for overly broad headers
        if (corsProperties.getAllowedHeaders().contains("*")) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.MEDIUM,
                "SECURITY_WILDCARD_HEADERS",
                "Wildcard headers (*) may expose unnecessary request headers",
                "Specify explicit headers instead of using wildcard",
                "Principle of Least Privilege"
            ));
        }
        
        // Check for missing security headers
        if (!corsProperties.getExposedHeaders().contains("X-Content-Type-Options")) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "SECURITY_MISSING_HEADERS",
                "Missing security headers in exposed headers",
                "Add security headers to exposed headers list",
                "Defense in Depth"
            ));
        }
    }

    /**
     * Validate OWASP CORS guidelines.
     */
    private void validateOWASPGuidelines(List<ComplianceIssue> issues) {
        // Check origin validation
        if (corsProperties.getAllowedOrigins().isEmpty()) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.CRITICAL,
                "OWASP_EMPTY_ORIGINS",
                "Empty allowed origins list effectively blocks all CORS requests",
                "Configure appropriate allowed origins",
                "OWASP CORS Configuration Guide"
            ));
        }
        
        // Check preflight cache duration
        if (corsProperties.getMaxAge() > 86400) { // 24 hours
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.MEDIUM,
                "OWASP_LONG_CACHE",
                "Preflight cache duration exceeds recommended maximum of 24 hours",
                "Reduce maxAge to 86400 seconds or less",
                "OWASP CORS Security Best Practices"
            ));
        }
        
        // Check for development origins in production
        if (isProductionEnvironment()) {
            long devOrigins = corsProperties.getAllowedOrigins().stream()
                    .filter(origin -> origin.contains("localhost") || 
                                    origin.contains(".local") || 
                                    origin.contains(".dev"))
                    .count();
            
            if (devOrigins > 0) {
                issues.add(new ComplianceIssue(
                    ComplianceIssue.Severity.HIGH,
                    "OWASP_DEV_ORIGINS_PROD",
                    "Development origins detected in production environment",
                    "Remove development origins from production configuration",
                    "Environment Separation Best Practices"
                ));
            }
        }
    }

    /**
     * Validate configuration consistency.
     */
    private void validateConfigurationConsistency(List<ComplianceIssue> issues) {
        // Check for duplicate origins
        long uniqueOrigins = corsProperties.getAllowedOrigins().stream().distinct().count();
        if (uniqueOrigins != corsProperties.getAllowedOrigins().size()) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "CONFIG_DUPLICATE_ORIGINS",
                "Duplicate origins found in configuration",
                "Remove duplicate origin entries",
                "Configuration Management"
            ));
        }
        
        // Check for null or empty origins
        long invalidOrigins = corsProperties.getAllowedOrigins().stream()
                .filter(origin -> origin == null || origin.trim().isEmpty())
                .count();
        
        if (invalidOrigins > 0) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.MEDIUM,
                "CONFIG_INVALID_ORIGINS",
                "Invalid (null or empty) origins found in configuration",
                "Remove invalid origin entries",
                "Configuration Validation"
            ));
        }
        
        // Check method consistency
        if (!corsProperties.getAllowedMethods().contains("OPTIONS")) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.MEDIUM,
                "CONFIG_MISSING_OPTIONS",
                "OPTIONS method not in allowed methods list",
                "Add OPTIONS to allowed methods for preflight requests",
                "CORS Standard Compliance"
            ));
        }
    }

    /**
     * Validate performance recommendations.
     */
    private void validatePerformanceRecommendations(List<ComplianceIssue> issues) {
        // Check preflight cache efficiency
        if (corsProperties.getMaxAge() < 300) { // 5 minutes
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "PERF_SHORT_CACHE",
                "Preflight cache duration is very short, may impact performance",
                "Consider increasing maxAge to at least 300 seconds",
                "Performance Optimization"
            ));
        }
        
        // Check for excessive origins
        if (corsProperties.getAllowedOrigins().size() > 20) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "PERF_MANY_ORIGINS",
                "Large number of allowed origins may impact validation performance",
                "Review and consolidate origins where possible",
                "Performance Best Practices"
            ));
        }
        
        // Check for excessive headers
        if (corsProperties.getAllowedHeaders().size() > 15) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "PERF_MANY_HEADERS",
                "Large number of allowed headers may impact preflight performance",
                "Review and minimize allowed headers",
                "Performance Optimization"
            ));
        }
    }

    /**
     * Validate operational requirements.
     */
    private void validateOperationalRequirements(List<ComplianceIssue> issues) {
        // Check if CORS is enabled
        if (!corsProperties.isEnabled()) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.MEDIUM,
                "OPS_CORS_DISABLED",
                "CORS is disabled - may block legitimate cross-origin requests",
                "Enable CORS if cross-origin requests are expected",
                "Operational Requirements"
            ));
        }
        
        // Check for monitoring requirements
        if (corsProperties.getExposedHeaders().stream()
                .noneMatch(header -> header.contains("Correlation") || header.contains("Request-Id"))) {
            issues.add(new ComplianceIssue(
                ComplianceIssue.Severity.LOW,
                "OPS_MISSING_TRACING",
                "No correlation/tracing headers exposed for debugging",
                "Add correlation ID headers to exposed headers",
                "Observability Best Practices"
            ));
        }
    }

    /**
     * Log compliance summary.
     */
    private void logComplianceSummary(ComplianceReport report) {
        int score = getComplianceScore();
        
        if (report.getIssues().isEmpty()) {
            logger.info("CORS compliance validation PASSED - Score: {}/100", score);
        } else {
            logger.warn("CORS compliance validation found {} issues - Score: {}/100", 
                       report.getIssues().size(), score);
            
            // Log critical and high issues
            report.getCriticalIssues().forEach(issue -> 
                logger.error("CRITICAL: {} - {}", issue.getCode(), issue.getDescription()));
            
            report.getHighIssues().forEach(issue -> 
                logger.warn("HIGH: {} - {}", issue.getCode(), issue.getDescription()));
        }
    }

    /**
     * Record compliance metrics.
     */
    private void recordComplianceMetrics(ComplianceReport report) {
        // Record issue counts by severity
        report.getCriticalIssues().forEach(issue -> 
            metricsCollector.recordSecurityViolation("compliance", "critical_issue", "critical"));
        
        report.getHighIssues().forEach(issue -> 
            metricsCollector.recordSecurityViolation("compliance", "high_issue", "high"));
        
        report.getMediumIssues().forEach(issue -> 
            metricsCollector.recordSecurityViolation("compliance", "medium_issue", "medium"));
        
        report.getLowIssues().forEach(issue -> 
            metricsCollector.recordSecurityViolation("compliance", "low_issue", "low"));
    }

    /**
     * Check if running in production environment.
     */
    private boolean isProductionEnvironment() {
        // Simple check - could be enhanced with actual environment detection
        return corsProperties.getAllowedOrigins().stream()
                .anyMatch(origin -> origin.contains(".com") || origin.contains(".org") || origin.contains(".net"));
    }

    /**
     * Compliance report containing all validation results.
     */
    public static class ComplianceReport {
        private final List<ComplianceIssue> issues;
        private final Instant timestamp;

        public ComplianceReport(List<ComplianceIssue> issues) {
            this.issues = new ArrayList<>(issues);
            this.timestamp = Instant.now();
        }

        public List<ComplianceIssue> getIssues() {
            return new ArrayList<>(issues);
        }

        public List<ComplianceIssue> getCriticalIssues() {
            return issues.stream()
                    .filter(issue -> issue.getSeverity() == ComplianceIssue.Severity.CRITICAL)
                    .collect(java.util.stream.Collectors.toList());
        }

        public List<ComplianceIssue> getHighIssues() {
            return issues.stream()
                    .filter(issue -> issue.getSeverity() == ComplianceIssue.Severity.HIGH)
                    .collect(java.util.stream.Collectors.toList());
        }

        public List<ComplianceIssue> getMediumIssues() {
            return issues.stream()
                    .filter(issue -> issue.getSeverity() == ComplianceIssue.Severity.MEDIUM)
                    .collect(java.util.stream.Collectors.toList());
        }

        public List<ComplianceIssue> getLowIssues() {
            return issues.stream()
                    .filter(issue -> issue.getSeverity() == ComplianceIssue.Severity.LOW)
                    .collect(java.util.stream.Collectors.toList());
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public boolean isCompliant() {
            return getCriticalIssues().isEmpty() && getHighIssues().isEmpty();
        }
    }

    /**
     * Individual compliance issue.
     */
    public static class ComplianceIssue {
        public enum Severity {
            CRITICAL, HIGH, MEDIUM, LOW
        }

        private final Severity severity;
        private final String code;
        private final String description;
        private final String recommendation;
        private final String standard;
        private final Instant detectedAt;

        public ComplianceIssue(Severity severity, String code, String description, 
                             String recommendation, String standard) {
            this.severity = severity;
            this.code = code;
            this.description = description;
            this.recommendation = recommendation;
            this.standard = standard;
            this.detectedAt = Instant.now();
        }

        // Getters
        public Severity getSeverity() { return severity; }
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getRecommendation() { return recommendation; }
        public String getStandard() { return standard; }
        public Instant getDetectedAt() { return detectedAt; }

        @Override
        public String toString() {
            return String.format("%s: %s - %s (Standard: %s)", 
                               severity, code, description, standard);
        }
    }
}
