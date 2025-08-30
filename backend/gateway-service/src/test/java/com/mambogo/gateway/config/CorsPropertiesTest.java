package com.mambogo.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CORS properties configuration.
 * 
 * @author Prashant Sinha
 * @since SEC-06 Implementation
 */
class CorsPropertiesTest {

    private CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
    }

    @Test
    @DisplayName("Should have default values")
    void shouldHaveDefaultValues() {
        // Then
        assertTrue(corsProperties.isEnabled());
        assertTrue(corsProperties.isAllowCredentials());
        assertEquals(3600L, corsProperties.getMaxAge());
        assertNotNull(corsProperties.getAllowedOrigins());
        assertNotNull(corsProperties.getAllowedMethods());
        assertNotNull(corsProperties.getAllowedHeaders());
    }

    @Test
    @DisplayName("Should configure production origins")
    void shouldConfigureProductionOrigins() {
        // Given
        List<String> productionOrigins = Arrays.asList(
            "https://www.mambogo.com",
            "https://mambogo.com"
        );
        
        // When
        corsProperties.setAllowedOrigins(productionOrigins);
        
        // Then
        assertEquals(2, corsProperties.getAllowedOrigins().size());
        assertTrue(corsProperties.getAllowedOrigins().contains("https://www.mambogo.com"));
        assertTrue(corsProperties.getAllowedOrigins().contains("https://mambogo.com"));
    }

    @Test
    @DisplayName("Should configure local development origins")
    void shouldConfigureLocalDevelopmentOrigins() {
        // Given
        List<String> localOrigins = Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:4173"
        );
        
        // When
        corsProperties.setAllowedOrigins(localOrigins);
        
        // Then
        assertEquals(3, corsProperties.getAllowedOrigins().size());
        assertTrue(corsProperties.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(corsProperties.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(corsProperties.getAllowedOrigins().contains("http://localhost:4173"));
    }

    @Test
    @DisplayName("Should configure allowed methods")
    void shouldConfigureAllowedMethods() {
        // Given
        List<String> methods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        
        // When
        corsProperties.setAllowedMethods(methods);
        
        // Then
        assertEquals(6, corsProperties.getAllowedMethods().size());
        assertTrue(corsProperties.getAllowedMethods().containsAll(methods));
    }

    @Test
    @DisplayName("Should configure security headers")
    void shouldConfigureSecurityHeaders() {
        // Given
        List<String> headers = Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Idempotency-Key",
            "X-Correlation-ID"
        );
        
        // When
        corsProperties.setAllowedHeaders(headers);
        
        // Then
        assertTrue(corsProperties.getAllowedHeaders().containsAll(headers));
    }

    @Test
    @DisplayName("Should configure credentials support")
    void shouldConfigureCredentialsSupport() {
        // When
        corsProperties.setAllowCredentials(false);
        
        // Then
        assertFalse(corsProperties.isAllowCredentials());
        
        // When
        corsProperties.setAllowCredentials(true);
        
        // Then
        assertTrue(corsProperties.isAllowCredentials());
    }

    @Test
    @DisplayName("Should configure max age")
    void shouldConfigureMaxAge() {
        // When
        corsProperties.setMaxAge(7200L);
        
        // Then
        assertEquals(7200L, corsProperties.getMaxAge());
    }

    @Test
    @DisplayName("Should be disableable")
    void shouldBeDisableable() {
        // When
        corsProperties.setEnabled(false);
        
        // Then
        assertFalse(corsProperties.isEnabled());
    }

    @Test
    @DisplayName("Should provide string representation")
    void shouldProvideStringRepresentation() {
        // When
        String toString = corsProperties.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("CorsProperties"));
        assertTrue(toString.contains("allowedOrigins"));
        assertTrue(toString.contains("allowCredentials"));
    }
}
