package com.mambogo.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class JwtClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private final OAuth2TokenValidator<Jwt> delegate;

    public JwtClaimsValidator(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        // Use basic timestamp validator for now
        this.delegate = new JwtTimestampValidator();
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        OAuth2TokenValidatorResult result = delegate.validate(jwt);
        
        if (result.hasErrors()) {
            return result;
        }
        
        // Additional custom validations
        if (!isTokenNotExpired(jwt)) {
            return OAuth2TokenValidatorResult.failure();
        }
        
        if (!hasRequiredClaims(jwt)) {
            return OAuth2TokenValidatorResult.failure();
        }
        
        return OAuth2TokenValidatorResult.success();
    }
    
    private boolean isTokenNotExpired(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
    
    private boolean hasRequiredClaims(Jwt jwt) {
        // Check for required claims
        return jwt.getSubject() != null && 
               jwt.getIssuer() != null &&
               jwt.getClaim("preferred_username") != null;
    }
}
