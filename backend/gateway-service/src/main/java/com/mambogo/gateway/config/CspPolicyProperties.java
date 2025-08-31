package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for Content Security Policy (CSP) management.
 * Provides comprehensive CSP policy configuration with environment-specific settings,
 * violation reporting, and security monitoring capabilities.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@ConfigurationProperties(prefix = "mambogo.csp")
public class CspPolicyProperties {

    /**
     * Whether CSP enforcement is enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to use report-only mode (for testing CSP policies).
     */
    private boolean reportOnly = false;

    /**
     * CSP violation reporting endpoint.
     */
    private String reportUri = "/api/csp/violations";

    /**
     * Whether to include CSP violation reporting.
     */
    private boolean violationReportingEnabled = true;

    /**
     * Maximum number of violations to track per origin.
     */
    private int maxViolationsPerOrigin = 100;

    /**
     * CSP policy directives configuration.
     */
    private PolicyDirectives directives = new PolicyDirectives();

    /**
     * Environment-specific CSP configurations.
     */
    private Map<String, PolicyDirectives> environmentPolicies = Map.of();

    /**
     * Whether to enable nonce generation for inline scripts/styles.
     */
    private boolean nonceEnabled = true;

    /**
     * Nonce generation algorithm (default: secure random).
     */
    private String nonceAlgorithm = "SHA-256";

    /**
     * Length of generated nonces in bytes.
     */
    private int nonceLength = 32;

    /**
     * Whether to enable hash generation for inline scripts/styles.
     */
    private boolean hashEnabled = false;

    /**
     * Hash algorithm for inline content (sha256, sha384, sha512).
     */
    private String hashAlgorithm = "sha256";

    /**
     * Whether to enable CSP metrics collection.
     */
    private boolean metricsEnabled = true;

    /**
     * Whether to enable performance monitoring for CSP.
     */
    private boolean performanceMonitoringEnabled = true;

    /**
     * CSP policy directives configuration class.
     */
    public static class PolicyDirectives {
        
        /**
         * Default source policy for all resource types.
         */
        private List<String> defaultSrc = List.of("'self'");

        /**
         * Script source policy.
         */
        private List<String> scriptSrc = List.of("'self'");

        /**
         * Style source policy.
         */
        private List<String> styleSrc = List.of("'self'");

        /**
         * Image source policy.
         */
        private List<String> imgSrc = List.of("'self'", "data:", "https:");

        /**
         * Connection source policy (AJAX, WebSocket, EventSource).
         */
        private List<String> connectSrc = List.of("'self'");

        /**
         * Font source policy.
         */
        private List<String> fontSrc = List.of("'self'");

        /**
         * Object source policy (plugins like Flash).
         */
        private List<String> objectSrc = List.of("'none'");

        /**
         * Media source policy (audio, video).
         */
        private List<String> mediaSrc = List.of("'self'");

        /**
         * Child source policy (frames, workers).
         */
        private List<String> childSrc = List.of("'self'");

        /**
         * Frame source policy (iframe sources).
         */
        private List<String> frameSrc = List.of("'none'");

        /**
         * Frame ancestors policy (what can embed this page).
         */
        private List<String> frameAncestors = List.of("'none'");

        /**
         * Form action policy (form submission targets).
         */
        private List<String> formAction = List.of("'self'");

        /**
         * Base URI policy.
         */
        private List<String> baseUri = List.of("'self'");

        /**
         * Manifest source policy (web app manifest).
         */
        private List<String> manifestSrc = List.of("'self'");

        /**
         * Worker source policy (service workers, shared workers).
         */
        private List<String> workerSrc = List.of("'self'");

        /**
         * Plugin types policy (deprecated but included for completeness).
         */
        private List<String> pluginTypes = List.of();

        /**
         * Whether to upgrade insecure requests.
         */
        private boolean upgradeInsecureRequests = true;

        /**
         * Whether to block all mixed content.
         */
        private boolean blockAllMixedContent = false;

        /**
         * Require SRI (Subresource Integrity) for scripts and links.
         */
        private boolean requireSriFor = false;

        /**
         * SRI resource types to require.
         */
        private List<String> sriResourceTypes = List.of("script", "style");

        // Constructors
        public PolicyDirectives() {}

        // Getters and Setters
        public List<String> getDefaultSrc() {
            return defaultSrc;
        }

        public void setDefaultSrc(List<String> defaultSrc) {
            this.defaultSrc = defaultSrc;
        }

        public List<String> getScriptSrc() {
            return scriptSrc;
        }

        public void setScriptSrc(List<String> scriptSrc) {
            this.scriptSrc = scriptSrc;
        }

        public List<String> getStyleSrc() {
            return styleSrc;
        }

        public void setStyleSrc(List<String> styleSrc) {
            this.styleSrc = styleSrc;
        }

        public List<String> getImgSrc() {
            return imgSrc;
        }

        public void setImgSrc(List<String> imgSrc) {
            this.imgSrc = imgSrc;
        }

        public List<String> getConnectSrc() {
            return connectSrc;
        }

        public void setConnectSrc(List<String> connectSrc) {
            this.connectSrc = connectSrc;
        }

        public List<String> getFontSrc() {
            return fontSrc;
        }

        public void setFontSrc(List<String> fontSrc) {
            this.fontSrc = fontSrc;
        }

        public List<String> getObjectSrc() {
            return objectSrc;
        }

        public void setObjectSrc(List<String> objectSrc) {
            this.objectSrc = objectSrc;
        }

        public List<String> getMediaSrc() {
            return mediaSrc;
        }

        public void setMediaSrc(List<String> mediaSrc) {
            this.mediaSrc = mediaSrc;
        }

        public List<String> getChildSrc() {
            return childSrc;
        }

        public void setChildSrc(List<String> childSrc) {
            this.childSrc = childSrc;
        }

        public List<String> getFrameSrc() {
            return frameSrc;
        }

        public void setFrameSrc(List<String> frameSrc) {
            this.frameSrc = frameSrc;
        }

        public List<String> getFrameAncestors() {
            return frameAncestors;
        }

        public void setFrameAncestors(List<String> frameAncestors) {
            this.frameAncestors = frameAncestors;
        }

        public List<String> getFormAction() {
            return formAction;
        }

        public void setFormAction(List<String> formAction) {
            this.formAction = formAction;
        }

        public List<String> getBaseUri() {
            return baseUri;
        }

        public void setBaseUri(List<String> baseUri) {
            this.baseUri = baseUri;
        }

        public List<String> getManifestSrc() {
            return manifestSrc;
        }

        public void setManifestSrc(List<String> manifestSrc) {
            this.manifestSrc = manifestSrc;
        }

        public List<String> getWorkerSrc() {
            return workerSrc;
        }

        public void setWorkerSrc(List<String> workerSrc) {
            this.workerSrc = workerSrc;
        }

        public List<String> getPluginTypes() {
            return pluginTypes;
        }

        public void setPluginTypes(List<String> pluginTypes) {
            this.pluginTypes = pluginTypes;
        }

        public boolean isUpgradeInsecureRequests() {
            return upgradeInsecureRequests;
        }

        public void setUpgradeInsecureRequests(boolean upgradeInsecureRequests) {
            this.upgradeInsecureRequests = upgradeInsecureRequests;
        }

        public boolean isBlockAllMixedContent() {
            return blockAllMixedContent;
        }

        public void setBlockAllMixedContent(boolean blockAllMixedContent) {
            this.blockAllMixedContent = blockAllMixedContent;
        }

        public boolean isRequireSriFor() {
            return requireSriFor;
        }

        public void setRequireSriFor(boolean requireSriFor) {
            this.requireSriFor = requireSriFor;
        }

        public List<String> getSriResourceTypes() {
            return sriResourceTypes;
        }

        public void setSriResourceTypes(List<String> sriResourceTypes) {
            this.sriResourceTypes = sriResourceTypes;
        }
    }

    // Constructors
    public CspPolicyProperties() {}

    // Main Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReportOnly() {
        return reportOnly;
    }

    public void setReportOnly(boolean reportOnly) {
        this.reportOnly = reportOnly;
    }

    public String getReportUri() {
        return reportUri;
    }

    public void setReportUri(String reportUri) {
        this.reportUri = reportUri;
    }

    public boolean isViolationReportingEnabled() {
        return violationReportingEnabled;
    }

    public void setViolationReportingEnabled(boolean violationReportingEnabled) {
        this.violationReportingEnabled = violationReportingEnabled;
    }

    public int getMaxViolationsPerOrigin() {
        return maxViolationsPerOrigin;
    }

    public void setMaxViolationsPerOrigin(int maxViolationsPerOrigin) {
        this.maxViolationsPerOrigin = maxViolationsPerOrigin;
    }

    public PolicyDirectives getDirectives() {
        return directives;
    }

    public void setDirectives(PolicyDirectives directives) {
        this.directives = directives;
    }

    public Map<String, PolicyDirectives> getEnvironmentPolicies() {
        return environmentPolicies;
    }

    public void setEnvironmentPolicies(Map<String, PolicyDirectives> environmentPolicies) {
        this.environmentPolicies = environmentPolicies;
    }

    public boolean isNonceEnabled() {
        return nonceEnabled;
    }

    public void setNonceEnabled(boolean nonceEnabled) {
        this.nonceEnabled = nonceEnabled;
    }

    public String getNonceAlgorithm() {
        return nonceAlgorithm;
    }

    public void setNonceAlgorithm(String nonceAlgorithm) {
        this.nonceAlgorithm = nonceAlgorithm;
    }

    public int getNonceLength() {
        return nonceLength;
    }

    public void setNonceLength(int nonceLength) {
        this.nonceLength = nonceLength;
    }

    public boolean isHashEnabled() {
        return hashEnabled;
    }

    public void setHashEnabled(boolean hashEnabled) {
        this.hashEnabled = hashEnabled;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }

    public void setPerformanceMonitoringEnabled(boolean performanceMonitoringEnabled) {
        this.performanceMonitoringEnabled = performanceMonitoringEnabled;
    }

    @Override
    public String toString() {
        return "CspPolicyProperties{" +
                "enabled=" + enabled +
                ", reportOnly=" + reportOnly +
                ", reportUri='" + reportUri + '\'' +
                ", violationReportingEnabled=" + violationReportingEnabled +
                ", maxViolationsPerOrigin=" + maxViolationsPerOrigin +
                ", directives=" + directives +
                ", nonceEnabled=" + nonceEnabled +
                ", hashEnabled=" + hashEnabled +
                ", metricsEnabled=" + metricsEnabled +
                '}';
    }
}
