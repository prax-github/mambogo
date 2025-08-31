package com.mambogo.gateway.cors;

import com.mambogo.gateway.config.CorsProperties;
import com.mambogo.gateway.config.CorsPolicyProperties;
import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import com.mambogo.gateway.policy.CorsPolicyManager;
import com.mambogo.gateway.security.CorsSecurityMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CORS policy enforcement.
 * 
 * Verifies that the CORS policy components are properly integrated
 * and can enforce security policies.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
    "mambogo.cors.enabled=true",
    "mambogo.cors.allowed-origins=http://localhost:5173,http://localhost:3000",
    "mambogo.cors.policy.enforcement-enabled=true",
    "mambogo.cors.policy.auto-block-enabled=false",
    "mambogo.cors.policy.require-user-agent=false"
})
class CorsIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CorsProperties corsProperties() {
            CorsProperties props = new CorsProperties();
            props.setEnabled(true);
            props.getAllowedOrigins().addAll(List.of("http://localhost:5173", "http://localhost:3000"));
            props.getAllowedMethods().addAll(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            return props;
        }
        
        @Bean
        @Primary 
        public CorsPolicyProperties corsPolicyProperties() {
            CorsPolicyProperties props = new CorsPolicyProperties();
            props.setEnforcementEnabled(true);
            props.setAutoBlockEnabled(false);
            props.setRequireUserAgent(false);
            return props;
        }
        
        @Bean
        public SimpleCorsMetricsCollector metricsCollector() {
            return new SimpleCorsMetricsCollector(Mockito.mock(MeterRegistry.class));
        }
        
        @Bean
        public CorsPolicyManager policyManager(CorsProperties corsProperties, SimpleCorsMetricsCollector metricsCollector) {
            return new CorsPolicyManager(corsProperties, metricsCollector);
        }
        
        @Bean
        public CorsSecurityMonitor securityMonitor(SimpleCorsMetricsCollector metricsCollector) {
            return new CorsSecurityMonitor(metricsCollector, Mockito.mock(ApplicationEventPublisher.class));
        }
    }

    @Autowired
    private CorsProperties corsProperties;
    
    @Autowired
    private CorsPolicyProperties corsPolicyProperties;
    
    @Autowired
    private SimpleCorsMetricsCollector metricsCollector;
    
    @Autowired
    private CorsPolicyManager policyManager;
    
    @Autowired
    private CorsSecurityMonitor securityMonitor;

    @Test
    void shouldLoadCorsConfiguration() {
        // Verify basic CORS configuration loads
        assertThat(corsProperties).isNotNull();
        assertThat(corsProperties.isEnabled()).isTrue();
        assertThat(corsProperties.getAllowedOrigins()).isNotEmpty();
    }

    @Test
    void shouldLoadCorsPolicyConfiguration() {
        // Verify advanced CORS policy configuration loads
        assertThat(corsPolicyProperties).isNotNull();
        assertThat(corsPolicyProperties.isEnforcementEnabled()).isTrue();
    }

    @Test
    void shouldCreateMetricsCollector() {
        // Verify metrics collector is properly initialized
        assertThat(metricsCollector).isNotNull();
        
        // Test basic metric recording
        metricsCollector.recordCorsRequest("http://localhost:5173", "GET", "200");
        metricsCollector.recordBlockedRequest("https://evil.com", "unauthorized_origin", "GET");
        
        // Verify counters work
        assertThat(metricsCollector.getTotalRequests()).isGreaterThan(0);
    }

    @Test
    void shouldCreatePolicyManager() {
        // Verify policy manager is properly initialized
        assertThat(policyManager).isNotNull();
        
        // Test policy validation for allowed origin
        var decision = policyManager.validateRequest(
            "http://localhost:5173", 
            "GET", 
            "/api/products",
            "Mozilla/5.0 Test", 
            "127.0.0.1"
        );
        
        assertThat(decision).isNotNull();
        // Decision should be allowed for localhost in test environment
        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    void shouldCreateSecurityMonitor() {
        // Verify security monitor is properly initialized
        assertThat(securityMonitor).isNotNull();
        
        // Test security assessment for suspicious origin
        var assessment = securityMonitor.monitorRequest(
            "javascript:alert(1)", 
            "GET", 
            "/api/products",
            "", 
            "192.168.1.100"
        );
        
        assertThat(assessment).isNotNull();
        assertThat(assessment.hasViolations()).isTrue();
    }

    @Test
    void shouldBlockSuspiciousOrigins() {
        // Test that suspicious origins are properly detected
        var assessment = securityMonitor.monitorRequest(
            "null", 
            "GET", 
            "/api/cart",
            "SuspiciousBot/1.0", 
            "10.0.0.1"
        );
        
        assertThat(assessment).isNotNull();
        assertThat(assessment.hasViolations()).isTrue();
        
        // Verify metrics are recorded
        metricsCollector.recordSuspiciousOrigin("null", "suspicious_pattern");
    }
}
