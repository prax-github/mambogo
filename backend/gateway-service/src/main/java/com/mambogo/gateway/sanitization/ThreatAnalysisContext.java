package com.mambogo.gateway.sanitization;

/**
 * Context information for threat analysis including endpoint, origin, and request metadata.
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
public class ThreatAnalysisContext {
    
    private final String endpoint;
    private final String origin;
    private final String userAgent;
    private final String method;
    private final String path;
    private final String userId;
    private final boolean authenticated;

    private ThreatAnalysisContext(Builder builder) {
        this.endpoint = builder.endpoint;
        this.origin = builder.origin;
        this.userAgent = builder.userAgent;
        this.method = builder.method;
        this.path = builder.path;
        this.userId = builder.userId;
        this.authenticated = builder.authenticated;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getOrigin() {
        return origin;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String endpoint;
        private String origin;
        private String userAgent;
        private String method;
        private String path;
        private String userId;
        private boolean authenticated = false;

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder authenticated(boolean authenticated) {
            this.authenticated = authenticated;
            return this;
        }

        public ThreatAnalysisContext build() {
            return new ThreatAnalysisContext(this);
        }
    }
}
