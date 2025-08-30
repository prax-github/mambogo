package com.mambogo.cart.config;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom expression root for method security that provides scope-based authorization methods
 */
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;
    private final JwtProperties jwtProperties;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, JwtProperties jwtProperties) {
        super(authentication);
        this.jwtProperties = jwtProperties;
    }

    /**
     * Check if the current user has the specified scope
     */
    public boolean hasScope(String requiredScope) {
        if (!(getAuthentication() instanceof JwtAuthenticationToken)) {
            return false;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) getAuthentication();
        return extractScopesFromJwt(jwtAuth.getToken()).contains(requiredScope);
    }

    /**
     * Check if the current user has any of the specified scopes
     */
    public boolean hasAnyScope(String... requiredScopes) {
        if (!(getAuthentication() instanceof JwtAuthenticationToken)) {
            return false;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) getAuthentication();
        Set<String> userScopes = extractScopesFromJwt(jwtAuth.getToken());
        
        return Arrays.stream(requiredScopes)
                .anyMatch(userScopes::contains);
    }

    /**
     * Check if the current user has all of the specified scopes
     */
    public boolean hasAllScopes(String... requiredScopes) {
        if (!(getAuthentication() instanceof JwtAuthenticationToken)) {
            return false;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) getAuthentication();
        Set<String> userScopes = extractScopesFromJwt(jwtAuth.getToken());
        
        return Arrays.stream(requiredScopes)
                .allMatch(userScopes::contains);
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

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return this;
    }
}
