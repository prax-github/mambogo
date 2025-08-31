package com.mambogo.gateway.config;

import com.mambogo.gateway.sanitization.InputSanitizationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for input sanitization middleware.
 * Enables configuration properties and registers required beans.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Configuration
@EnableConfigurationProperties(InputSanitizationProperties.class)
public class InputSanitizationConfiguration {
    
    // Configuration is handled through component scanning and @ConfigurationProperties
    // All required beans are annotated with @Component and will be auto-registered
}
