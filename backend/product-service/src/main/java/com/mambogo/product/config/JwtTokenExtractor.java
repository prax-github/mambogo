package com.mambogo.product.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtTokenExtractor {

    private final JwtProperties jwtProperties;

    public JwtTokenExtractor(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Extract user ID from JWT token
     */
    public Optional<String> getUserId() {
        return getJwt()
                .map(jwt -> jwt.getSubject());
    }

    /**
     * Extract username from JWT token
     */
    public Optional<String> getUsername() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString(jwtProperties.getClaims().getPreferredUsername()));
    }

    /**
     * Extract user roles from JWT token
     */
    
    public List<String> getUserRoles() {
        return getJwt()
                .map(jwt -> {
                    Map<String, Object> realmAccess = jwt.getClaimAsMap(jwtProperties.getClaims().getRealmAccess());
                    if (realmAccess != null) {
                        Object rolesObj = realmAccess.get(jwtProperties.getClaims().getRoles());
                        if (rolesObj instanceof List) {
                            List<?> rolesList = (List<?>) rolesObj;
                            return rolesList.stream()
                                    .map(Object::toString)
                                    .collect(java.util.stream.Collectors.toList());
                        }
                    }
                    return new ArrayList<String>();
                })
                .orElse(new ArrayList<>());
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return getUserRoles().contains(role);
    }

    /**
     * Check if user has admin role
     */
    public boolean isAdmin() {
        return hasRole(jwtProperties.getRoles().getAdmin());
    }

    /**
     * Check if user has user role
     */
    public boolean isUser() {
        return hasRole(jwtProperties.getRoles().getUser());
    }

    /**
     * Extract email from JWT token
     */
    public Optional<String> getEmail() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString(jwtProperties.getClaims().getEmail()));
    }

    /**
     * Extract full name from JWT token
     */
    public Optional<String> getFullName() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString(jwtProperties.getClaims().getFullName()));
    }

    /**
     * Get JWT token from security context
     */
    private Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return Optional.of((Jwt) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Get raw JWT token string
     */
    public Optional<String> getRawToken() {
        return getJwt()
                .map(Jwt::getTokenValue);
    }
}
