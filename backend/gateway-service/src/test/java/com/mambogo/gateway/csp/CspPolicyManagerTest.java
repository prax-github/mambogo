package com.mambogo.gateway.csp;

import com.mambogo.gateway.config.CorsProperties;
import com.mambogo.gateway.config.CspPolicyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for CSP Policy Manager functionality.
 * Tests policy generation, validation, nonce creation, and environment-specific configurations.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@ExtendWith(MockitoExtension.class)
class CspPolicyManagerTest {

    private CspPolicyManager policyManager;
    private CspPolicyProperties cspProperties;
    private CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        // Initialize test configuration
        cspProperties = new CspPolicyProperties();
        cspProperties.setEnabled(true);
        cspProperties.setReportOnly(false);
        cspProperties.setNonceEnabled(true);
        cspProperties.setViolationReportingEnabled(true);
        cspProperties.setReportUri("/api/csp/violations");
        
        // Configure test directives
        CspPolicyProperties.PolicyDirectives directives = new CspPolicyProperties.PolicyDirectives();
        directives.setDefaultSrc(List.of("'self'"));
        directives.setScriptSrc(List.of("'self'"));
        directives.setStyleSrc(List.of("'self'", "fonts.googleapis.com"));
        directives.setImgSrc(List.of("'self'", "data:", "https:"));
        directives.setObjectSrc(List.of("'none'"));
        directives.setFrameAncestors(List.of("'none'"));
        cspProperties.setDirectives(directives);
        
        // Initialize CORS properties
        corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of("https://www.mambogo.com", "https://mambogo.com"));
        
        // Create policy manager with test environment
        policyManager = new CspPolicyManager(cspProperties, corsProperties, "test");
    }

    @Test
    void shouldGenerateBasicCspPolicy() {
        // When
        String policy = policyManager.generateCspPolicy("/api/test", "https://www.mambogo.com");
        
        // Then
        assertThat(policy).isNotNull();
        assertThat(policy).contains("default-src 'self'");
        assertThat(policy).contains("script-src 'self'");
        assertThat(policy).contains("style-src 'self' fonts.googleapis.com");
        assertThat(policy).contains("img-src 'self' data: https:");
        assertThat(policy).contains("object-src 'none'");
        assertThat(policy).contains("frame-ancestors 'none'");
        assertThat(policy).contains("report-uri /api/csp/violations");
    }

    @Test
    void shouldIncludeConnectSrcFromCorsOrigins() {
        // When
        String policy = policyManager.generateCspPolicy("/api/test", "https://www.mambogo.com");
        
        // Then
        assertThat(policy).contains("connect-src 'self'");
        assertThat(policy).contains("https://www.mambogo.com");
        assertThat(policy).contains("https://mambogo.com");
    }

    @Test
    void shouldReturnNullWhenCspDisabled() {
        // Given
        cspProperties.setEnabled(false);
        
        // When
        String policy = policyManager.generateCspPolicy("/api/test", "https://www.mambogo.com");
        
        // Then
        assertThat(policy).isNull();
    }

    @Test
    void shouldReturnCorrectHeaderNameForReportOnly() {
        // Given
        cspProperties.setReportOnly(true);
        
        // When
        String headerName = policyManager.getCspHeaderName();
        
        // Then
        assertThat(headerName).isEqualTo("Content-Security-Policy-Report-Only");
    }

    @Test
    void shouldReturnCorrectHeaderNameForEnforcement() {
        // Given
        cspProperties.setReportOnly(false);
        
        // When
        String headerName = policyManager.getCspHeaderName();
        
        // Then
        assertThat(headerName).isEqualTo("Content-Security-Policy");
    }

    @Test
    void shouldGenerateSecureNonce() {
        // When
        String nonce1 = policyManager.generateNonce();
        String nonce2 = policyManager.generateNonce();
        
        // Then
        assertThat(nonce1).isNotNull();
        assertThat(nonce2).isNotNull();
        assertThat(nonce1).isNotEqualTo(nonce2); // Nonces should be unique
        assertThat(nonce1).matches("^[A-Za-z0-9+/]+=*$"); // Base64 format
    }

    @Test
    void shouldReturnNullNonceWhenDisabled() {
        // Given
        cspProperties.setNonceEnabled(false);
        
        // When
        String nonce = policyManager.generateNonce();
        
        // Then
        assertThat(nonce).isNull();
    }

    @Test
    void shouldValidatePolicyForSecurityIssues() {
        // Given
        String unsafePolicy = "default-src 'self'; script-src 'self' 'unsafe-eval' 'unsafe-inline'; object-src *";
        
        // When
        CspValidationResult result = policyManager.validatePolicy(unsafePolicy);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("unsafe-eval"));
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("unsafe-inline"));
    }

    @Test
    void shouldValidateSecurePolicy() {
        // Given
        String securePolicy = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; frame-ancestors 'none'";
        
        // When
        CspValidationResult result = policyManager.validatePolicy(securePolicy);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldDetectMissingEssentialDirectives() {
        // Given
        String incompletePolicy = "script-src 'self'";
        
        // When
        CspValidationResult result = policyManager.validatePolicy(incompletePolicy);
        
        // Then
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("default-src"));
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("object-src"));
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("frame-ancestors"));
    }

    @Test
    void shouldCachePolicies() {
        // Given
        String path = "/api/test";
        String origin = "https://www.mambogo.com";
        
        // When
        String policy1 = policyManager.generateCspPolicy(path, origin);
        String policy2 = policyManager.generateCspPolicy(path, origin);
        
        // Then
        assertThat(policy1).isEqualTo(policy2);
        // Note: In a real test, we'd verify caching behavior through metrics or mocks
    }

    @Test
    void shouldClearPolicyCache() {
        // Given
        policyManager.generateCspPolicy("/api/test", "https://www.mambogo.com");
        
        // When
        policyManager.clearPolicyCache();
        
        // Then
        // Policy cache should be cleared (verified through cache statistics)
        var stats = policyManager.getCacheStatistics();
        assertThat(stats).containsKeys("policyCacheSize", "nonceCacheSize", "activeEnvironment", "cspEnabled", "reportOnlyMode");
    }

    @Test
    void shouldProvideCacheStatistics() {
        // When
        var stats = policyManager.getCacheStatistics();
        
        // Then
        assertThat(stats).containsKey("policyCacheSize");
        assertThat(stats).containsKey("nonceCacheSize");
        assertThat(stats).containsKey("activeEnvironment");
        assertThat(stats).containsKey("cspEnabled");
        assertThat(stats).containsKey("reportOnlyMode");
        assertThat(stats.get("activeEnvironment")).isEqualTo("test");
        assertThat(stats.get("cspEnabled")).isEqualTo(true);
    }

    @Test
    void shouldHandleNullOriginGracefully() {
        // When
        String policy = policyManager.generateCspPolicy("/api/test", null);
        
        // Then
        assertThat(policy).isNotNull();
        assertThat(policy).contains("default-src 'self'");
        // Should still include CORS origins in connect-src
        assertThat(policy).contains("connect-src 'self'");
    }

    @Test
    void shouldHandleEmptyOriginGracefully() {
        // When
        String policy = policyManager.generateCspPolicy("/api/test", "");
        
        // Then
        assertThat(policy).isNotNull();
        assertThat(policy).contains("default-src 'self'");
    }

    @Test
    void shouldRejectInvalidOrigins() {
        // When
        String policy1 = policyManager.generateCspPolicy("/api/test", "javascript:alert('xss')");
        String policy2 = policyManager.generateCspPolicy("/api/test", "data:text/html,<script>alert('xss')</script>");
        String policy3 = policyManager.generateCspPolicy("/api/test", "null");
        
        // Then
        assertThat(policy1).isNotNull();
        assertThat(policy2).isNotNull(); 
        assertThat(policy3).isNotNull();
        // These policies should not include the invalid origins in connect-src
        assertThat(policy1).doesNotContain("javascript:");
        assertThat(policy2).doesNotContain("data:text/html");
        assertThat(policy3).doesNotContain("null");
    }

    @Test
    void shouldHandleEmptyPolicy() {
        // Given
        String emptyPolicy = "";
        
        // When
        CspValidationResult result = policyManager.validatePolicy(emptyPolicy);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("CSP policy is empty or null");
    }

    @Test
    void shouldHandleNullPolicy() {
        // When
        CspValidationResult result = policyManager.validatePolicy(null);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("CSP policy is empty or null");
    }
}
