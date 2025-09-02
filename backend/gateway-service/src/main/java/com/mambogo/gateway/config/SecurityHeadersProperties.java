package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Configuration properties for comprehensive security headers baseline.
 * 
 * This class provides centralized configuration for all security headers including:
 * - MIME type protection (X-Content-Type-Options)
 * - Clickjacking protection (X-Frame-Options, CSP frame-ancestors)
 * - XSS protection (X-XSS-Protection, CSP script-src)
 * - Privacy and control (Referrer-Policy, Permissions-Policy)
 * - HTTPS enforcement (Strict-Transport-Security)
 * - Additional protection headers
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Component
@ConfigurationProperties(prefix = "mambogo.security-headers")
public class SecurityHeadersProperties {

    /**
     * Whether security headers are enabled globally.
     */
    private boolean enabled = true;

    /**
     * Whether to enable report-only mode for testing.
     */
    private boolean reportOnly = false;

    /**
     * Environment-specific security level.
     */
    private SecurityLevel securityLevel = SecurityLevel.PRODUCTION;

    /**
     * MIME type sniffing protection configuration.
     */
    private MimeTypeProtection mimeTypeProtection = new MimeTypeProtection();

    /**
     * Clickjacking protection configuration.
     */
    private ClickjackingProtection clickjackingProtection = new ClickjackingProtection();

    /**
     * XSS protection configuration.
     */
    private XssProtection xssProtection = new XssProtection();

    /**
     * Referrer policy configuration.
     */
    private ReferrerPolicy referrerPolicy = new ReferrerPolicy();

    /**
     * HTTPS enforcement configuration.
     */
    private HttpsEnforcement httpsEnforcement = new HttpsEnforcement();

    /**
     * Feature control configuration.
     */
    private FeatureControl featureControl = new FeatureControl();

    /**
     * Additional security headers configuration.
     */
    private AdditionalHeaders additionalHeaders = new AdditionalHeaders();

    /**
     * Monitoring and validation configuration.
     */
    private Monitoring monitoring = new Monitoring();

    // Enums
    public enum SecurityLevel {
        LOCAL,      // Development environment - relaxed security
        DEMO,       // Demo environment - balanced security
        PRODUCTION  // Production environment - maximum security
    }

    // Nested configuration classes
    public static class MimeTypeProtection {
        private boolean enabled = true;
        private String value = "nosniff";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ClickjackingProtection {
        private boolean enabled = true;
        private String xFrameOptions = "DENY";
        private List<String> frameAncestors = List.of("'none'");
        private boolean allowSameOrigin = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getXFrameOptions() { return xFrameOptions; }
        public void setXFrameOptions(String xFrameOptions) { this.xFrameOptions = xFrameOptions; }
        public List<String> getFrameAncestors() { return frameAncestors; }
        public void setFrameAncestors(List<String> frameAncestors) { this.frameAncestors = frameAncestors; }
        public boolean isAllowSameOrigin() { return allowSameOrigin; }
        public void setAllowSameOrigin(boolean allowSameOrigin) { this.allowSameOrigin = allowSameOrigin; }
    }

    public static class XssProtection {
        private boolean enabled = true;
        private String value = "1; mode=block";
        private boolean enableCspScriptSrc = true;
        private List<String> scriptSrc = List.of("'self'");
        private boolean allowUnsafeInline = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isEnableCspScriptSrc() { return enableCspScriptSrc; }
        public void setEnableCspScriptSrc(boolean enableCspScriptSrc) { this.enableCspScriptSrc = enableCspScriptSrc; }
        public List<String> getScriptSrc() { return scriptSrc; }
        public void setScriptSrc(List<String> scriptSrc) { this.scriptSrc = scriptSrc; }
        public boolean isAllowUnsafeInline() { return allowUnsafeInline; }
        public void setAllowUnsafeInline(boolean allowUnsafeInline) { this.allowUnsafeInline = allowUnsafeInline; }
    }

    public static class ReferrerPolicy {
        private boolean enabled = true;
        private String value = "strict-origin-when-cross-origin";
        private boolean allowDowngrade = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isAllowDowngrade() { return allowDowngrade; }
        public void setAllowDowngrade(boolean allowDowngrade) { this.allowDowngrade = allowDowngrade; }
    }

    public static class HttpsEnforcement {
        private boolean enabled = true;
        private long maxAge = 31536000; // 1 year
        private boolean includeSubDomains = true;
        private boolean preload = true;
        private boolean forceHttps = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
        public boolean isIncludeSubDomains() { return includeSubDomains; }
        public void setIncludeSubDomains(boolean includeSubDomains) { this.includeSubDomains = includeSubDomains; }
        public boolean isPreload() { return preload; }
        public void setPreload(boolean preload) { this.preload = preload; }
        public boolean isForceHttps() { return forceHttps; }
        public void setForceHttps(boolean forceHttps) { this.forceHttps = forceHttps; }
    }

    public static class FeatureControl {
        private boolean enabled = true;
        private Set<String> disabledFeatures = Set.of("camera", "microphone", "geolocation", "payment");
        private boolean allowSelf = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Set<String> getDisabledFeatures() { return disabledFeatures; }
        public void setDisabledFeatures(Set<String> disabledFeatures) { this.disabledFeatures = disabledFeatures; }
        public boolean isAllowSelf() { return allowSelf; }
        public void setAllowSelf(boolean allowSelf) { this.allowSelf = allowSelf; }
    }

    public static class AdditionalHeaders {
        private boolean enabled = true;
        private boolean dnsPrefetchControl = true;
        private String dnsPrefetchValue = "off";
        private boolean crossDomainPolicies = true;
        private String crossDomainValue = "none";
        private boolean poweredBy = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isDnsPrefetchControl() { return dnsPrefetchControl; }
        public void setDnsPrefetchControl(boolean dnsPrefetchControl) { this.dnsPrefetchControl = dnsPrefetchControl; }
        public String getDnsPrefetchValue() { return dnsPrefetchValue; }
        public void setDnsPrefetchValue(String dnsPrefetchValue) { this.dnsPrefetchValue = dnsPrefetchValue; }
        public boolean isCrossDomainPolicies() { return crossDomainPolicies; }
        public void setCrossDomainPolicies(boolean crossDomainPolicies) { this.crossDomainPolicies = crossDomainPolicies; }
        public String getCrossDomainValue() { return crossDomainValue; }
        public void setCrossDomainValue(String crossDomainValue) { this.crossDomainValue = crossDomainValue; }
        public boolean isPoweredBy() { return poweredBy; }
        public void setPoweredBy(boolean poweredBy) { this.poweredBy = poweredBy; }
    }

    public static class Monitoring {
        private boolean enabled = true;
        private boolean enableMetrics = true;
        private boolean enableValidation = true;
        private boolean enableAuditLogging = true;
        private long validationInterval = 300000; // 5 minutes

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        public boolean isEnableValidation() { return enableValidation; }
        public void setEnableValidation(boolean enableValidation) { this.enableValidation = enableValidation; }
        public boolean isEnableAuditLogging() { return enableAuditLogging; }
        public void setEnableAuditLogging(boolean enableAuditLogging) { this.enableAuditLogging = enableAuditLogging; }
        public long getValidationInterval() { return validationInterval; }
        public void setValidationInterval(long validationInterval) { this.validationInterval = validationInterval; }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isReportOnly() { return reportOnly; }
    public void setReportOnly(boolean reportOnly) { this.reportOnly = reportOnly; }

    public SecurityLevel getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(SecurityLevel securityLevel) { this.securityLevel = securityLevel; }

    public MimeTypeProtection getMimeTypeProtection() { return mimeTypeProtection; }
    public void setMimeTypeProtection(MimeTypeProtection mimeTypeProtection) { this.mimeTypeProtection = mimeTypeProtection; }

    public ClickjackingProtection getClickjackingProtection() { return clickjackingProtection; }
    public void setClickjackingProtection(ClickjackingProtection clickjackingProtection) { this.clickjackingProtection = clickjackingProtection; }

    public XssProtection getXssProtection() { return xssProtection; }
    public void setXssProtection(XssProtection xssProtection) { this.xssProtection = xssProtection; }

    public ReferrerPolicy getReferrerPolicy() { return referrerPolicy; }
    public void setReferrerPolicy(ReferrerPolicy referrerPolicy) { this.referrerPolicy = referrerPolicy; }

    public HttpsEnforcement getHttpsEnforcement() { return httpsEnforcement; }
    public void setHttpsEnforcement(HttpsEnforcement httpsEnforcement) { this.httpsEnforcement = httpsEnforcement; }

    public FeatureControl getFeatureControl() { return featureControl; }
    public void setFeatureControl(FeatureControl featureControl) { this.featureControl = featureControl; }

    public AdditionalHeaders getAdditionalHeaders() { return additionalHeaders; }
    public void setAdditionalHeaders(AdditionalHeaders additionalHeaders) { this.additionalHeaders = additionalHeaders; }

    public Monitoring getMonitoring() { return monitoring; }
    public void setMonitoring(Monitoring monitoring) { this.monitoring = monitoring; }

    /**
     * Gets the effective security level based on environment.
     */
    public SecurityLevel getEffectiveSecurityLevel() {
        if (securityLevel == SecurityLevel.LOCAL && isProductionEnvironment()) {
            return SecurityLevel.PRODUCTION;
        }
        return securityLevel;
    }

    /**
     * Checks if the current environment is production.
     */
    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active");
        return profile != null && (profile.contains("prod") || profile.contains("production"));
    }

    /**
     * Validates the configuration for security compliance.
     */
    public void validateConfiguration() {
        if (securityLevel == SecurityLevel.PRODUCTION) {
            if (xssProtection.isAllowUnsafeInline()) {
                throw new IllegalStateException("Production security level does not allow unsafe-inline");
            }
            if (!httpsEnforcement.isForceHttps()) {
                throw new IllegalStateException("Production security level requires HTTPS enforcement");
            }
        }
    }
}
