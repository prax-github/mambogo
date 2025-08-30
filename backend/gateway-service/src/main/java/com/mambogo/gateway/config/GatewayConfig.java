package com.mambogo.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Public routes (no authentication required)
            .route("public-catalog", r -> r
                .path("/api/catalog/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                )
                .uri("lb://product-service"))
            
            .route("public-products", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                )
                .uri("lb://product-service"))
            
            // Secured routes (authentication required)
            .route("secured-cart", r -> r
                .path("/api/cart/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "cart-service")
                )
                .uri("lb://cart-service"))
            
            .route("secured-orders", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "order-service")
                )
                .uri("lb://order-service"))
            
            .route("secured-payments", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "payment-service")
                )
                .uri("lb://payment-service"))
            
            // Admin routes (admin role required)
            .route("admin-routes", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Admin-Route", "true")
                    .addRequestHeader("X-Service-Name", "admin-service")
                )
                .uri("lb://admin-service"))
            
            .build();
    }
}
