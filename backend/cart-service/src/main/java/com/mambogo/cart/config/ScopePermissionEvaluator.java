package com.mambogo.cart.config;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom permission evaluator for OAuth2 scope-based authorization
 */
@Component
public class ScopePermissionEvaluator implements PermissionEvaluator {

    private final JwtProperties jwtProperties;

    public ScopePermissionEvaluator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (!(authentication instanceof JwtAuthenticationToken)) {
            return false;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        String requiredScope = permission.toString();
        
        return hasScope(jwtAuth.getToken(), requiredScope);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }

    /**
     * Check if JWT token contains the required scope
     */
    private boolean hasScope(Jwt jwt, String requiredScope) {
        Set<String> scopes = extractScopesFromJwt(jwt);
        return scopes.contains(requiredScope);
    }

    /**
     * Extract scopes from JWT token
     */
    private Set<String> extractScopesFromJwt(Jwt jwt) {
        String scopeClaim = jwt.getClaimAsString(jwtProperties.getClaims().getScope());
        if (scopeClaim == null || scopeClaim.trim().isEmpty()) {
            return new HashSet<>();
        }

        // Scopes in JWT are typically space-separated
        return Arrays.stream(scopeClaim.split("\\s+"))
                .filter(scope -> !scope.trim().isEmpty())
                .collect(Collectors.toSet());
    }
}
