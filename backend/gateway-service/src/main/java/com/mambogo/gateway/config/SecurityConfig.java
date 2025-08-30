package com.mambogo.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (no authentication required)
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/api/catalog/**").permitAll()
                .pathMatchers("/api/products/**").permitAll()
                
                // Secured endpoints with role requirements
                .pathMatchers("/api/cart/**").hasRole("USER")
                .pathMatchers("/api/orders/**").hasRole("USER")
                .pathMatchers("/api/payments/**").hasRole("USER")
                
                // Admin endpoints
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Default: require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            );
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if (!corsProperties.isEnabled()) {
            logger.warn("CORS is disabled. This should only be used in specific environments.");
            return exchange -> null;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from configuration
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        logger.info("CORS configured with allowed origins: {}", corsProperties.getAllowedOrigins());
        
        // Set allowed methods
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        
        // Set allowed headers
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        
        // Set exposed headers
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        
        // Set credentials support
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        
        // Set max age for preflight cache
        configuration.setMaxAge(corsProperties.getMaxAge());
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        logger.info("CORS configuration initialized successfully");
        return source;
    }

    /**
     * Validates CORS configuration for security compliance.
     */
    private void validateCorsConfiguration(CorsConfiguration configuration) {
        // Security validation: ensure no wildcard origins with credentials
        Boolean allowCredentials = configuration.getAllowCredentials();
        if (allowCredentials != null && allowCredentials.booleanValue()) {
            List<String> allowedOrigins = configuration.getAllowedOrigins();
            if (allowedOrigins != null) {
                for (String origin : allowedOrigins) {
                    if ("*".equals(origin)) {
                        throw new IllegalArgumentException(
                            "Cannot use wildcard origin '*' with allowCredentials=true. " +
                            "This is a security vulnerability. Specify explicit origins.");
                    }
                }
            }
        }
        
        // Log security-relevant configuration
        logger.info("CORS Security Validation - Allow Credentials: {}, Origins: {}", 
                   allowCredentials, 
                   configuration.getAllowedOrigins());
    }

    @Bean
    public JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        return grantedAuthoritiesConverter;
    }

    public static class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
        
        @Override
        public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> errorResponse = Map.of(
                "code", "AUTHENTICATION_FAILED",
                "message", "Authentication required",
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
            );
            
            try {
                return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(
                        new ObjectMapper().writeValueAsBytes(errorResponse)
                    ))
                );
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }
    }

    public static class CustomAccessDeniedHandler implements org.springframework.security.web.server.authorization.ServerAccessDeniedHandler {
        
        @Override
        public Mono<Void> handle(ServerWebExchange exchange, org.springframework.security.access.AccessDeniedException e) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> errorResponse = Map.of(
                "code", "AUTHORIZATION_FAILED",
                "message", "Insufficient permissions",
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value()
            );
            
            try {
                return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(
                        new ObjectMapper().writeValueAsBytes(errorResponse)
                    ))
                );
            } catch (Exception ex) {
                return Mono.error(ex);
            }
        }
    }
}
