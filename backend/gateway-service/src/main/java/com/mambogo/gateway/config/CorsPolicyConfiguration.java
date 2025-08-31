package com.mambogo.gateway.config;

import com.mambogo.gateway.audit.CorsAuditLogger;
import com.mambogo.gateway.metrics.SimpleCorsMetricsCollector;
import com.mambogo.gateway.monitoring.CorsPerformanceMonitor;
import com.mambogo.gateway.policy.CorsComplianceValidator;
import com.mambogo.gateway.policy.CorsPolicyManager;
import com.mambogo.gateway.security.CorsAlertManager;
import com.mambogo.gateway.security.CorsSecurityMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for advanced CORS policy management.
 * 
 * Configures all CORS policy, monitoring, and security components.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Configuration
@EnableConfigurationProperties({CorsProperties.class, CorsPolicyProperties.class})
public class CorsPolicyConfiguration {

    @Bean
    public SimpleCorsMetricsCollector corsMetricsCollector(MeterRegistry meterRegistry) {
        return new SimpleCorsMetricsCollector(meterRegistry);
    }

    @Bean
    public CorsPerformanceMonitor corsPerformanceMonitor(SimpleCorsMetricsCollector metricsCollector, 
                                                        MeterRegistry meterRegistry) {
        return new CorsPerformanceMonitor(metricsCollector, meterRegistry);
    }

    @Bean
    public CorsSecurityMonitor corsSecurityMonitor(SimpleCorsMetricsCollector metricsCollector,
                                                   ApplicationEventPublisher eventPublisher) {
        return new CorsSecurityMonitor(metricsCollector, eventPublisher);
    }

    @Bean
    public CorsAlertManager corsAlertManager(SimpleCorsMetricsCollector metricsCollector) {
        return new CorsAlertManager(metricsCollector);
    }

    @Bean
    public CorsPolicyManager corsPolicyManager(CorsProperties corsProperties,
                                              SimpleCorsMetricsCollector metricsCollector) {
        return new CorsPolicyManager(corsProperties, metricsCollector);
    }

    @Bean
    public CorsComplianceValidator corsComplianceValidator(CorsProperties corsProperties,
                                                          SimpleCorsMetricsCollector metricsCollector) {
        return new CorsComplianceValidator(corsProperties, metricsCollector);
    }

    @Bean
    public CorsAuditLogger corsAuditLogger() {
        return new CorsAuditLogger();
    }
}
