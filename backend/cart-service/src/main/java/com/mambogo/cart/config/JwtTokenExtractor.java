package com.mambogo.cart.config;

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
                .map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Extract user roles from JWT token
     */
    // @SuppressWarnings("unchecked")
    public List<String> getUserRoles() {
        return getJwt()
                .map(jwt -> {
                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    if (realmAccess != null) {
                        Object rolesObj = realmAccess.get("roles");
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
        return hasRole("ADMIN");
    }

    /**
     * Check if user has user role
     */
    public boolean isUser() {
        return hasRole("USER");
    }

    /**
     * Extract email from JWT token
     */
    public Optional<String> getEmail() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Extract full name from JWT token
     */
    public Optional<String> getFullName() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("name"));
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
