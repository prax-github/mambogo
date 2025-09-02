package com.mambogo.gateway.config;

import com.mambogo.gateway.security.SecurityHeadersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;

/**
 * Spring configuration for the comprehensive security headers system.
 * 
 * This configuration:
 * - Enables configuration properties for security headers
 * - Creates and configures the SecurityHeadersManager
 * - Validates configuration on startup
 * - Provides centralized security headers management
 * 
 * @author Prashant Sinha
 * @since CON-06 Implementation
 */
@Configuration
@EnableConfigurationProperties(SecurityHeadersProperties.class)
public class SecurityHeadersConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersConfiguration.class);
    
    private final SecurityHeadersProperties properties;

    public SecurityHeadersConfiguration(SecurityHeadersProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing Security Headers Configuration...");
        
        try {
            // Validate configuration
            properties.validateConfiguration();
            
            // Log configuration summary
            logConfigurationSummary();
            
            logger.info("Security Headers Configuration initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Security Headers Configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Security Headers Configuration initialization failed", e);
        }
    }

    /**
     * Creates the primary SecurityHeadersManager bean.
     */
    @Bean
    @Primary
    public SecurityHeadersManager securityHeadersManager() {
        logger.info("Creating SecurityHeadersManager with security level: {}", 
                   properties.getEffectiveSecurityLevel());
        
        SecurityHeadersManager manager = new SecurityHeadersManager(properties);
        
        // Validate the manager configuration
        manager.validateConfiguration();
        
        return manager;
    }

    /**
     * Logs the configuration summary for debugging and audit purposes.
     */
    private void logConfigurationSummary() {
        logger.info("=== Security Headers Configuration Summary ===");
        logger.info("Enabled: {}", properties.isEnabled());
        logger.info("Security Level: {}", properties.getEffectiveSecurityLevel());
        logger.info("Report Only Mode: {}", properties.isReportOnly());
        
        // Security features status
        logger.info("--- Security Features Status ---");
        logger.info("MIME Type Protection: {}", properties.getMimeTypeProtection().isEnabled());
        logger.info("Clickjacking Protection: {}", properties.getClickjackingProtection().isEnabled());
        logger.info("XSS Protection: {}", properties.getXssProtection().isEnabled());
        logger.info("Referrer Policy: {}", properties.getReferrerPolicy().isEnabled());
        logger.info("HTTPS Enforcement: {}", properties.getHttpsEnforcement().isEnabled());
        logger.info("Feature Control: {}", properties.getFeatureControl().isEnabled());
        logger.info("Additional Headers: {}", properties.getAdditionalHeaders().isEnabled());
        
        // Monitoring configuration
        logger.info("--- Monitoring Configuration ---");
        logger.info("Metrics Enabled: {}", properties.getMonitoring().isEnableMetrics());
        logger.info("Validation Enabled: {}", properties.getMonitoring().isEnableValidation());
        logger.info("Audit Logging: {}", properties.getMonitoring().isEnableAuditLogging());
        logger.info("Validation Interval: {}ms", properties.getMonitoring().getValidationInterval());
        
        // Environment-specific settings
        logger.info("--- Environment Settings ---");
        logger.info("Production Environment: {}", isProductionEnvironment());
        logger.info("HTTPS Force: {}", properties.getHttpsEnforcement().isForceHttps());
        logger.info("Allow Unsafe Inline: {}", properties.getXssProtection().isAllowUnsafeInline());
        
        logger.info("=============================================");
    }

    /**
     * Checks if the current environment is production.
     */
    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active");
        return profile != null && (profile.contains("prod") || profile.contains("production"));
    }
}
