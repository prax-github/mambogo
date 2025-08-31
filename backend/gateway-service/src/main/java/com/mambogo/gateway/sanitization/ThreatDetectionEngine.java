package com.mambogo.gateway.sanitization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Advanced threat detection engine for identifying and scoring security threats
 * in request inputs. Provides real-time threat analysis with configurable policies.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Component
public class ThreatDetectionEngine {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    private final InputSanitizationProperties properties;
    
    // Threat detection patterns - compiled once for performance
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script[^>]*>.*?</script>|javascript:|vbscript:|data:text/html|on\\w+\\s*=|<iframe|<object|<embed|<link|<meta|<style>.*?</style>)"
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select|select.*from|insert\\s+into|update.*set|delete\\s+from|drop\\s+table|create\\s+table|alter\\s+table|exec\\s*\\(|execute\\s*\\(|sp_executesql)"
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c|%252e%252e%252f|%c0%ae%c0%ae%2f)"
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;\\s*|\\|\\s*|&\\s*|`|\\$\\(|\\${|<\\(|>\\(|\\s*(cat|ls|pwd|id|whoami|uname|nc|netcat|curl|wget|bash|sh|cmd|powershell)\\s)"
    );
    
    private static final Pattern SCRIPT_INJECTION_PATTERN = Pattern.compile(
        "(?i)(<\\?php|<%|%>|<\\?=|\\?>|eval\\s*\\(|base64_decode|exec\\s*\\(|system\\s*\\(|shell_exec)"
    );
    
    private static final Pattern DATA_EXFILTRATION_PATTERN = Pattern.compile(
        "(?i)(document\\.cookie|window\\.location|location\\.href|document\\.location|XMLHttpRequest|fetch\\s*\\(|ajax|\\.(jpg|png|gif|bmp)\\?)"
    );
    
    // Advanced patterns for sophisticated attacks
    private static final Pattern UNICODE_EVASION_PATTERN = Pattern.compile(
        "(%u[0-9a-fA-F]{4}|\\\\u[0-9a-fA-F]{4}|&#x[0-9a-fA-F]+;|&#[0-9]+;)"
    );
    
    private static final Pattern ENCODING_EVASION_PATTERN = Pattern.compile(
        "(%[0-9a-fA-F]{2}|\\+|%20|%0a|%0d|%00)"
    );

    // Threat tracking for rate limiting
    private final Map<String, ViolationTracker> violationTrackers = new ConcurrentHashMap<>();

    public ThreatDetectionEngine(InputSanitizationProperties properties) {
        this.properties = properties;
    }

    /**
     * Analyzes input for security threats and returns a threat score.
     * 
     * @param input The input to analyze
     * @param context Additional context (endpoint, origin, etc.)
     * @return ThreatAnalysisResult containing threat score and details
     */
    public ThreatAnalysisResult analyzeInput(String input, ThreatAnalysisContext context) {
        if (input == null || input.isEmpty()) {
            return new ThreatAnalysisResult(0, List.of(), false);
        }

        ThreatAnalysisResult.Builder resultBuilder = new ThreatAnalysisResult.Builder();
        
        // Run all threat detection checks
        if (properties.getThreatDetection().isEnableXssDetection()) {
            checkXssThreat(input, resultBuilder);
        }
        
        if (properties.getThreatDetection().isEnableSqlInjectionDetection()) {
            checkSqlInjectionThreat(input, resultBuilder);
        }
        
        if (properties.getThreatDetection().isEnablePathTraversalDetection()) {
            checkPathTraversalThreat(input, resultBuilder);
        }
        
        if (properties.getThreatDetection().isEnableCommandInjectionDetection()) {
            checkCommandInjectionThreat(input, resultBuilder);
        }
        
        if (properties.getThreatDetection().isEnableScriptInjectionDetection()) {
            checkScriptInjectionThreat(input, resultBuilder);
        }
        
        if (properties.getThreatDetection().isEnableDataExfiltrationDetection()) {
            checkDataExfiltrationThreat(input, resultBuilder);
        }
        
        // Advanced evasion detection
        checkEvasionTechniques(input, resultBuilder);
        
        // Anomaly detection based on input characteristics
        if (properties.getThreatDetection().isEnableAnomalyDetection()) {
            checkAnomalies(input, context, resultBuilder);
        }

        ThreatAnalysisResult result = resultBuilder.build();
        
        // Track violations for rate limiting
        if (result.isThreatDetected()) {
            trackViolation(context.getOrigin(), result);
        }
        
        // Log high-risk threats
        if (result.getThreatScore() >= properties.getThreatDetection().getThreatScoreThreshold()) {
            securityLogger.warn("HIGH-RISK THREAT DETECTED - Score: {}, Origin: {}, Endpoint: {}, Threats: {}, Input: {}", 
                result.getThreatScore(), context.getOrigin(), context.getEndpoint(), 
                result.getThreats(), sanitizeForLogging(input));
        }
        
        return result;
    }

    private void checkXssThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (XSS_PATTERN.matcher(input).find()) {
            builder.addThreat("XSS_INJECTION", 25, "Cross-site scripting pattern detected");
        }
    }

    private void checkSqlInjectionThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            builder.addThreat("SQL_INJECTION", 30, "SQL injection pattern detected");
        }
    }

    private void checkPathTraversalThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            builder.addThreat("PATH_TRAVERSAL", 20, "Path traversal pattern detected");
        }
    }

    private void checkCommandInjectionThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            builder.addThreat("COMMAND_INJECTION", 35, "Command injection pattern detected");
        }
    }

    private void checkScriptInjectionThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (SCRIPT_INJECTION_PATTERN.matcher(input).find()) {
            builder.addThreat("SCRIPT_INJECTION", 25, "Script injection pattern detected");
        }
    }

    private void checkDataExfiltrationThreat(String input, ThreatAnalysisResult.Builder builder) {
        if (DATA_EXFILTRATION_PATTERN.matcher(input).find()) {
            builder.addThreat("DATA_EXFILTRATION", 20, "Data exfiltration pattern detected");
        }
    }

    private void checkEvasionTechniques(String input, ThreatAnalysisResult.Builder builder) {
        if (UNICODE_EVASION_PATTERN.matcher(input).find()) {
            builder.addThreat("UNICODE_EVASION", 15, "Unicode evasion technique detected");
        }
        
        if (ENCODING_EVASION_PATTERN.matcher(input).find()) {
            builder.addThreat("ENCODING_EVASION", 10, "Encoding evasion technique detected");
        }
    }

    private void checkAnomalies(String input, ThreatAnalysisContext context, ThreatAnalysisResult.Builder builder) {
        // Check for suspicious length
        if (input.length() > 10000) {
            builder.addThreat("SUSPICIOUS_LENGTH", 10, "Abnormally long input detected");
        }
        
        // Check for high entropy (potential encrypted/encoded payload)
        double entropy = calculateEntropy(input);
        if (entropy > 4.5) {
            builder.addThreat("HIGH_ENTROPY", 15, "High entropy input suggesting encoded payload");
        }
        
        // Check for excessive special characters
        long specialCharCount = input.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        double specialCharRatio = (double) specialCharCount / input.length();
        if (specialCharRatio > 0.3) {
            builder.addThreat("EXCESSIVE_SPECIAL_CHARS", 10, "High ratio of special characters");
        }
        
        // Check for binary data patterns
        if (containsBinaryPatterns(input)) {
            builder.addThreat("BINARY_DATA", 20, "Binary data patterns detected in text input");
        }
    }

    private double calculateEntropy(String input) {
        Map<Character, Integer> charFreq = new ConcurrentHashMap<>();
        for (char c : input.toCharArray()) {
            charFreq.merge(c, 1, Integer::sum);
        }
        
        double entropy = 0.0;
        int length = input.length();
        for (int freq : charFreq.values()) {
            double probability = (double) freq / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        
        return entropy;
    }

    private boolean containsBinaryPatterns(String input) {
        // Check for null bytes and control characters
        return input.chars().anyMatch(ch -> ch < 32 && ch != 9 && ch != 10 && ch != 13);
    }

    private void trackViolation(String origin, ThreatAnalysisResult result) {
        ViolationTracker tracker = violationTrackers.computeIfAbsent(origin, k -> new ViolationTracker());
        tracker.recordViolation(result.getThreatScore());
        
        // Clean up old trackers
        if (violationTrackers.size() > 10000) {
            cleanupOldTrackers();
        }
    }

    private void cleanupOldTrackers() {
        long now = System.currentTimeMillis();
        long windowMs = properties.getViolationWindowSeconds() * 1000;
        
        violationTrackers.entrySet().removeIf(entry -> 
            now - entry.getValue().getLastViolationTime() > windowMs);
    }

    /**
     * Checks if an origin has exceeded violation thresholds
     */
    public boolean isOriginBlocked(String origin) {
        ViolationTracker tracker = violationTrackers.get(origin);
        if (tracker == null) {
            return false;
        }
        
        return tracker.getViolationCount() >= properties.getMaxViolationsPerOrigin();
    }

    /**
     * Gets violation statistics for an origin
     */
    public ViolationStats getViolationStats(String origin) {
        ViolationTracker tracker = violationTrackers.get(origin);
        if (tracker == null) {
            return new ViolationStats(0, 0, 0);
        }
        
        return new ViolationStats(
            tracker.getViolationCount(),
            tracker.getTotalThreatScore(),
            tracker.getLastViolationTime()
        );
    }

    private String sanitizeForLogging(String input) {
        if (input == null) return "null";
        
        String sanitized = input.replaceAll("[\\r\\n\\t]", " ");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        
        return sanitized;
    }

    // Inner classes for tracking and results
    private static class ViolationTracker {
        private int violationCount = 0;
        private int totalThreatScore = 0;
        private long lastViolationTime = 0;

        void recordViolation(int threatScore) {
            this.violationCount++;
            this.totalThreatScore += threatScore;
            this.lastViolationTime = System.currentTimeMillis();
        }

        int getViolationCount() { return violationCount; }
        int getTotalThreatScore() { return totalThreatScore; }
        long getLastViolationTime() { return lastViolationTime; }
    }

    public static class ViolationStats {
        private final int violationCount;
        private final int totalThreatScore;
        private final long lastViolationTime;

        public ViolationStats(int violationCount, int totalThreatScore, long lastViolationTime) {
            this.violationCount = violationCount;
            this.totalThreatScore = totalThreatScore;
            this.lastViolationTime = lastViolationTime;
        }

        public int getViolationCount() { return violationCount; }
        public int getTotalThreatScore() { return totalThreatScore; }
        public long getLastViolationTime() { return lastViolationTime; }
    }
}
