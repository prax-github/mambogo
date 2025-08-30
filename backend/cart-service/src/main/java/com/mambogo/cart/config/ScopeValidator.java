package com.mambogo.cart.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for validating OAuth2 scopes from JWT tokens
 */
@Component
public class ScopeValidator {

    private final JwtProperties jwtProperties;

    public ScopeValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Check if the current user has the specified scope
     */
    public boolean hasScope(String requiredScope) {
        return extractScopes().contains(requiredScope);
    }

    /**
     * Check if the current user has any of the specified scopes
     */
    public boolean hasAnyScope(String... requiredScopes) {
        Set<String> userScopes = extractScopes();
        return Arrays.stream(requiredScopes)
                .anyMatch(userScopes::contains);
    }

    /**
     * Check if the current user has all of the specified scopes
     */
    public boolean hasAllScopes(String... requiredScopes) {
        Set<String> userScopes = extractScopes();
        return Arrays.stream(requiredScopes)
                .allMatch(userScopes::contains);
    }

    /**
     * Get all scopes for the current user
     */
    public Set<String> getUserScopes() {
        return extractScopes();
    }

    /**
     * Check if user has product read scope
     */
    public boolean hasProductReadScope() {
        return hasScope(jwtProperties.getScopes().getProductRead());
    }

    /**
     * Check if user has cart manage scope
     */
    public boolean hasCartManageScope() {
        return hasScope(jwtProperties.getScopes().getCartManage());
    }

    /**
     * Check if user has order write scope
     */
    public boolean hasOrderWriteScope() {
        return hasScope(jwtProperties.getScopes().getOrderWrite());
    }

    /**
     * Check if user has payment process scope
     */
    public boolean hasPaymentProcessScope() {
        return hasScope(jwtProperties.getScopes().getPaymentProcess());
    }

    /**
     * Check if user has admin all scope
     */
    public boolean hasAdminAllScope() {
        return hasScope(jwtProperties.getScopes().getAdminAll());
    }

    /**
     * Extract scopes from the current JWT token
     */
    private Set<String> extractScopes() {
        return getJwt()
                .map(this::extractScopesFromJwt)
                .orElse(new HashSet<>());
    }

    /**
     * Extract scopes from a JWT token
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

    /**
     * Get the current JWT token from the security context
     */
    private Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            return Optional.of(jwtAuth.getToken());
        }
        return Optional.empty();
    }

    /**
     * Check if the JWT token has a valid audience claim for this service
     */
    public boolean hasValidAudience(String expectedAudience) {
        return getJwt()
                .map(jwt -> {
                    Object audienceClaim = jwt.getClaim(jwtProperties.getClaims().getAudience());
                    if (audienceClaim instanceof String) {
                        return expectedAudience.equals(audienceClaim);
                    } else if (audienceClaim instanceof java.util.List) {
                        java.util.List<?> audiences = (java.util.List<?>) audienceClaim;
                        return audiences.contains(expectedAudience);
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Validate scope and audience together
     */
    public boolean validateScopeAndAudience(String requiredScope, String expectedAudience) {
        return hasScope(requiredScope) && hasValidAudience(expectedAudience);
    }
}
