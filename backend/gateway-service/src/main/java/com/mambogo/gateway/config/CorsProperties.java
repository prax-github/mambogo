package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.List;

/**
 * Configuration properties for CORS settings across different environments.
 * 
 * @author Prashant Sinha
 * @since SEC-06 Implementation
 */
@ConfigurationProperties(prefix = "mambogo.cors")
public class CorsProperties {

    /**
     * List of allowed origins for CORS requests.
     * Environment-specific configuration:
     * - Local: http://localhost:5173, http://localhost:3000
     * - Demo: https://demo.mambogo.com
     * - Production: https://www.mambogo.com, https://mambogo.com
     */
    private List<String> allowedOrigins = List.of("http://localhost:5173");

    /**
     * List of allowed HTTP methods for CORS requests.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

    /**
     * List of allowed headers for CORS requests.
     * Using "*" allows all headers but specific headers are more secure.
     */
    private List<String> allowedHeaders = List.of(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "X-Idempotency-Key",
        "X-Correlation-ID"
    );

    /**
     * List of headers exposed to the client.
     */
    private List<String> exposedHeaders = List.of(
        "Access-Control-Allow-Origin",
        "Access-Control-Allow-Credentials",
        "X-Correlation-ID",
        "X-Rate-Limit-Remaining",
        "X-Rate-Limit-Retry-After-Seconds"
    );

    /**
     * Whether to allow credentials (cookies, authorization headers) in CORS requests.
     * Required for JWT token-based authentication.
     */
    private boolean allowCredentials = true;

    /**
     * Maximum age in seconds for preflight cache.
     * Default: 1 hour (3600 seconds)
     */
    private long maxAge = 3600L;

    /**
     * Whether CORS is enabled.
     * Useful for disabling CORS in specific environments if needed.
     */
    private boolean enabled = true;

    // Constructors
    public CorsProperties() {}

    // Getters and Setters
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "CorsProperties{" +
                "allowedOrigins=" + allowedOrigins +
                ", allowedMethods=" + allowedMethods +
                ", allowedHeaders=" + allowedHeaders +
                ", exposedHeaders=" + exposedHeaders +
                ", allowCredentials=" + allowCredentials +
                ", maxAge=" + maxAge +
                ", enabled=" + enabled +
                '}';
    }
}
