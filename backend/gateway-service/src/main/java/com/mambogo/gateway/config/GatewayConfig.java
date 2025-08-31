package com.mambogo.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mambogo.gateway.config.RateLimitConstants.*;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                         @Qualifier(IP_KEY_RESOLVER) KeyResolver ipKeyResolver,
                                         @Qualifier(USER_KEY_RESOLVER) KeyResolver userKeyResolver,
                                         @Qualifier(IP_RATE_LIMITER) RedisRateLimiter ipRateLimiter,
                                         @Qualifier(USER_RATE_LIMITER) RedisRateLimiter userRateLimiter) {
        return builder.routes()
            // Public routes (no authentication required) - IP-based rate limiting
            .route("public-catalog", r -> r
                .path("/api/catalog/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(ipRateLimiter)
                        .setKeyResolver(ipKeyResolver)
                    )
                )
                .uri("lb://product-service"))
            
            .route("public-products", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(ipRateLimiter)
                        .setKeyResolver(ipKeyResolver)
                    )
                )
                .uri("lb://product-service"))
            
            // Secured routes (authentication required) - User-based rate limiting
            .route("secured-cart", r -> r
                .path("/api/cart/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "cart-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(userRateLimiter)
                        .setKeyResolver(userKeyResolver)
                    )
                )
                .uri("lb://cart-service"))
            
            .route("secured-orders", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "order-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(userRateLimiter)
                        .setKeyResolver(userKeyResolver)
                    )
                )
                .uri("lb://order-service"))
            
            .route("secured-payments", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "payment-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(userRateLimiter)
                        .setKeyResolver(userKeyResolver)
                    )
                )
                .uri("lb://payment-service"))
            
            // Admin routes (admin role required) - User-based rate limiting
            .route("admin-routes", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Admin-Route", "true")
                    .addRequestHeader("X-Service-Name", "admin-service")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(userRateLimiter)
                        .setKeyResolver(userKeyResolver)
                    )
                )
                .uri("lb://admin-service"))
            
            .build();
    }
}
