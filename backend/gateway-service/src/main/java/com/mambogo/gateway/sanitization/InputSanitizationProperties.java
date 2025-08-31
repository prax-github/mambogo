package com.mambogo.gateway.sanitization;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for input sanitization middleware at the gateway level.
 * Provides endpoint-specific sanitization policies and threat detection settings.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Component
@ConfigurationProperties(prefix = "mambogo.gateway.sanitization")
public class InputSanitizationProperties {

    private boolean enabled = true;
    private boolean logThreats = true;
    private boolean blockSuspiciousRequests = true;
    private int maxViolationsPerOrigin = 100;
    private long violationWindowSeconds = 3600; // 1 hour
    
    // Performance settings
    private int maxRequestBodySize = 1024 * 1024; // 1MB
    private int maxParameterValueLength = 4096;
    private int maxParameterCount = 50;
    private boolean enableAsyncProcessing = true;
    
    // Threat detection settings
    private ThreatDetectionSettings threatDetection = new ThreatDetectionSettings();
    
    // Endpoint-specific policies
    private Map<String, EndpointPolicy> endpointPolicies = Map.of(
        "products", new EndpointPolicy("permissive", false, true, false, false),
        "cart", new EndpointPolicy("moderate", true, true, true, false),
        "orders", new EndpointPolicy("restrictive", true, true, true, true),
        "payments", new EndpointPolicy("strict", true, true, true, true)
    );
    
    // Whitelist patterns (bypass sanitization)
    private Set<String> whitelistPatterns = Set.of(
        "/actuator/.*",
        "/api/csp/.*",
        "/api/health/.*"
    );

    public static class ThreatDetectionSettings {
        private boolean enableXssDetection = true;
        private boolean enableSqlInjectionDetection = true;
        private boolean enablePathTraversalDetection = true;
        private boolean enableCommandInjectionDetection = true;
        private boolean enableScriptInjectionDetection = true;
        private boolean enableDataExfiltrationDetection = true;
        
        // Advanced threat detection
        private boolean enableAnomalyDetection = true;
        private boolean enablePatternLearning = false; // Future feature
        private int threatScoreThreshold = 75; // 0-100 scale
        
        // Getters and Setters
        public boolean isEnableXssDetection() {
            return enableXssDetection;
        }

        public void setEnableXssDetection(boolean enableXssDetection) {
            this.enableXssDetection = enableXssDetection;
        }

        public boolean isEnableSqlInjectionDetection() {
            return enableSqlInjectionDetection;
        }

        public void setEnableSqlInjectionDetection(boolean enableSqlInjectionDetection) {
            this.enableSqlInjectionDetection = enableSqlInjectionDetection;
        }

        public boolean isEnablePathTraversalDetection() {
            return enablePathTraversalDetection;
        }

        public void setEnablePathTraversalDetection(boolean enablePathTraversalDetection) {
            this.enablePathTraversalDetection = enablePathTraversalDetection;
        }

        public boolean isEnableCommandInjectionDetection() {
            return enableCommandInjectionDetection;
        }

        public void setEnableCommandInjectionDetection(boolean enableCommandInjectionDetection) {
            this.enableCommandInjectionDetection = enableCommandInjectionDetection;
        }

        public boolean isEnableScriptInjectionDetection() {
            return enableScriptInjectionDetection;
        }

        public void setEnableScriptInjectionDetection(boolean enableScriptInjectionDetection) {
            this.enableScriptInjectionDetection = enableScriptInjectionDetection;
        }

        public boolean isEnableDataExfiltrationDetection() {
            return enableDataExfiltrationDetection;
        }

        public void setEnableDataExfiltrationDetection(boolean enableDataExfiltrationDetection) {
            this.enableDataExfiltrationDetection = enableDataExfiltrationDetection;
        }

        public boolean isEnableAnomalyDetection() {
            return enableAnomalyDetection;
        }

        public void setEnableAnomalyDetection(boolean enableAnomalyDetection) {
            this.enableAnomalyDetection = enableAnomalyDetection;
        }

        public boolean isEnablePatternLearning() {
            return enablePatternLearning;
        }

        public void setEnablePatternLearning(boolean enablePatternLearning) {
            this.enablePatternLearning = enablePatternLearning;
        }

        public int getThreatScoreThreshold() {
            return threatScoreThreshold;
        }

        public void setThreatScoreThreshold(int threatScoreThreshold) {
            this.threatScoreThreshold = threatScoreThreshold;
        }
    }

    public static class EndpointPolicy {
        private String policyName;
        private boolean sanitizeRequestBody;
        private boolean sanitizeQueryParams;
        private boolean sanitizeHeaders;
        private boolean enableStrictMode;
        
        public EndpointPolicy() {}
        
        public EndpointPolicy(String policyName, boolean sanitizeRequestBody, 
                            boolean sanitizeQueryParams, boolean sanitizeHeaders, 
                            boolean enableStrictMode) {
            this.policyName = policyName;
            this.sanitizeRequestBody = sanitizeRequestBody;
            this.sanitizeQueryParams = sanitizeQueryParams;
            this.sanitizeHeaders = sanitizeHeaders;
            this.enableStrictMode = enableStrictMode;
        }

        // Getters and Setters
        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public boolean isSanitizeRequestBody() {
            return sanitizeRequestBody;
        }

        public void setSanitizeRequestBody(boolean sanitizeRequestBody) {
            this.sanitizeRequestBody = sanitizeRequestBody;
        }

        public boolean isSanitizeQueryParams() {
            return sanitizeQueryParams;
        }

        public void setSanitizeQueryParams(boolean sanitizeQueryParams) {
            this.sanitizeQueryParams = sanitizeQueryParams;
        }

        public boolean isSanitizeHeaders() {
            return sanitizeHeaders;
        }

        public void setSanitizeHeaders(boolean sanitizeHeaders) {
            this.sanitizeHeaders = sanitizeHeaders;
        }

        public boolean isEnableStrictMode() {
            return enableStrictMode;
        }

        public void setEnableStrictMode(boolean enableStrictMode) {
            this.enableStrictMode = enableStrictMode;
        }
    }

    // Main getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogThreats() {
        return logThreats;
    }

    public void setLogThreats(boolean logThreats) {
        this.logThreats = logThreats;
    }

    public boolean isBlockSuspiciousRequests() {
        return blockSuspiciousRequests;
    }

    public void setBlockSuspiciousRequests(boolean blockSuspiciousRequests) {
        this.blockSuspiciousRequests = blockSuspiciousRequests;
    }

    public int getMaxViolationsPerOrigin() {
        return maxViolationsPerOrigin;
    }

    public void setMaxViolationsPerOrigin(int maxViolationsPerOrigin) {
        this.maxViolationsPerOrigin = maxViolationsPerOrigin;
    }

    public long getViolationWindowSeconds() {
        return violationWindowSeconds;
    }

    public void setViolationWindowSeconds(long violationWindowSeconds) {
        this.violationWindowSeconds = violationWindowSeconds;
    }

    public int getMaxRequestBodySize() {
        return maxRequestBodySize;
    }

    public void setMaxRequestBodySize(int maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
    }

    public int getMaxParameterValueLength() {
        return maxParameterValueLength;
    }

    public void setMaxParameterValueLength(int maxParameterValueLength) {
        this.maxParameterValueLength = maxParameterValueLength;
    }

    public int getMaxParameterCount() {
        return maxParameterCount;
    }

    public void setMaxParameterCount(int maxParameterCount) {
        this.maxParameterCount = maxParameterCount;
    }

    public boolean isEnableAsyncProcessing() {
        return enableAsyncProcessing;
    }

    public void setEnableAsyncProcessing(boolean enableAsyncProcessing) {
        this.enableAsyncProcessing = enableAsyncProcessing;
    }

    public ThreatDetectionSettings getThreatDetection() {
        return threatDetection;
    }

    public void setThreatDetection(ThreatDetectionSettings threatDetection) {
        this.threatDetection = threatDetection;
    }

    public Map<String, EndpointPolicy> getEndpointPolicies() {
        return endpointPolicies;
    }

    public void setEndpointPolicies(Map<String, EndpointPolicy> endpointPolicies) {
        this.endpointPolicies = endpointPolicies;
    }

    public Set<String> getWhitelistPatterns() {
        return whitelistPatterns;
    }

    public void setWhitelistPatterns(Set<String> whitelistPatterns) {
        this.whitelistPatterns = whitelistPatterns;
    }
}
