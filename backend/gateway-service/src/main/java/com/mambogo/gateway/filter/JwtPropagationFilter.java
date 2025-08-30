package com.mambogo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import java.util.List;
import java.util.Map;

@Component
public class JwtPropagationFilter implements GlobalFilter, Ordered {

    private final JwtDecoder jwtDecoder;

    public JwtPropagationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Extract JWT token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                // Decode JWT to extract claims
                Jwt jwt = jwtDecoder.decode(token);
                
                // Extract user information from JWT claims
                String userId = extractUserIdFromJwt(jwt);
                String userRoles = extractRolesFromJwt(jwt);
                String username = extractUsernameFromJwt(jwt);
                
                // Add JWT token and user information to downstream service headers
                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-JWT-Token", token)
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", userRoles)
                    .header("X-Username", username)
                    .header("X-Authenticated", "true")
                    .build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                
            } catch (JwtException e) {
                // Log JWT decoding error but continue with the request
                // The security filter will handle authentication
                return chain.filter(exchange);
            }
        }
        
        // No JWT token found, continue with the request
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
    
    private String extractUserIdFromJwt(Jwt jwt) {
        try {
            return jwt.getSubject();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String extractRolesFromJwt(Jwt jwt) {
        try {
            // Extract roles from realm_access.roles claim
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null && !roles.isEmpty()) {
                    return String.join(",", roles);
                }
            }
            return "ROLE_USER";
        } catch (Exception e) {
            return "ROLE_USER";
        }
    }
    
    private String extractUsernameFromJwt(Jwt jwt) {
        try {
            return jwt.getClaim("preferred_username");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
