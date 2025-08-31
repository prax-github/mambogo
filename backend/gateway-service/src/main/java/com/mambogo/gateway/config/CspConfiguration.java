package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Content Security Policy (CSP) components.
 * Enables CSP configuration properties and ensures all CSP-related beans are properly configured.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@Configuration
@EnableConfigurationProperties({CspPolicyProperties.class})
public class CspConfiguration {
    
    // Configuration class to enable CSP properties
    // All CSP-related beans are automatically detected via @Component annotations
}
