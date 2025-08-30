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
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

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
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
