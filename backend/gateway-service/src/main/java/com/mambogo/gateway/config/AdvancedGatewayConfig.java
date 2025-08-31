package com.mambogo.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Advanced Gateway configuration with endpoint-specific rate limiting.
 * Provides granular rate limiting policies for different service endpoints
 * with advanced features like circuit breakers and adaptive thresholds.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Configuration
public class AdvancedGatewayConfig {

    /**
     * Advanced route locator with endpoint-specific rate limiting.
     * This replaces the basic GatewayConfig with more granular controls.
     */
    @Bean
    @Primary
    public RouteLocator advancedRouteLocator(
            RouteLocatorBuilder builder,
            // Basic key resolvers (from SEC-05)
            @Qualifier(IP_KEY_RESOLVER) KeyResolver ipKeyResolver,
            @Qualifier(USER_KEY_RESOLVER) KeyResolver userKeyResolver,
            // Advanced endpoint-aware key resolvers (SEC-10)
            @Qualifier(ENDPOINT_IP_KEY_RESOLVER) KeyResolver endpointIpKeyResolver,
            @Qualifier(ENDPOINT_USER_KEY_RESOLVER) KeyResolver endpointUserKeyResolver,
            // Basic rate limiters (from SEC-05)
            @Qualifier(IP_RATE_LIMITER) RedisRateLimiter ipRateLimiter,
            @Qualifier(USER_RATE_LIMITER) RedisRateLimiter userRateLimiter,
            // Advanced endpoint-specific rate limiters (SEC-10)
            @Qualifier(PRODUCTS_RATE_LIMITER) RedisRateLimiter productsRateLimiter,
            @Qualifier(CART_USER_RATE_LIMITER) RedisRateLimiter cartUserRateLimiter,
            @Qualifier(CART_IP_RATE_LIMITER) RedisRateLimiter cartIpRateLimiter,
            @Qualifier(ORDERS_USER_RATE_LIMITER) RedisRateLimiter ordersUserRateLimiter,
            @Qualifier(ORDERS_IP_RATE_LIMITER) RedisRateLimiter ordersIpRateLimiter,
            @Qualifier(PAYMENTS_USER_RATE_LIMITER) RedisRateLimiter paymentsUserRateLimiter,
            @Qualifier(PAYMENTS_IP_RATE_LIMITER) RedisRateLimiter paymentsIpRateLimiter) {

        return builder.routes()
            
            // PUBLIC ROUTES - Products/Catalog (Most permissive)
            .route("advanced-catalog", r -> r
                .path("/api/catalog/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "products-permissive")
                    // Use products-specific rate limiter with endpoint-aware key resolver
                    .requestRateLimiter(c -> c
                        .setRateLimiter(productsRateLimiter)
                        .setKeyResolver(endpointIpKeyResolver)
                    )
                )
                .uri("lb://product-service"))
            
            .route("advanced-products", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Public-Route", "true")
                    .addRequestHeader("X-Service-Name", "product-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "products-permissive")
                    // Use products-specific rate limiter with endpoint-aware key resolver
                    .requestRateLimiter(c -> c
                        .setRateLimiter(productsRateLimiter)
                        .setKeyResolver(endpointIpKeyResolver)
                    )
                )
                .uri("lb://product-service"))
            
            // SECURED ROUTES - Cart Service (Moderate limits)
            .route("advanced-cart", r -> r
                .path("/api/cart/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "cart-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "cart-moderate")
                    // Use cart-specific user rate limiter
                    .requestRateLimiter(c -> c
                        .setRateLimiter(cartUserRateLimiter)
                        .setKeyResolver(endpointUserKeyResolver)
                    )
                    // Add secondary IP-based rate limiting for cart
                    .requestRateLimiter(c -> c
                        .setRateLimiter(cartIpRateLimiter)
                        .setKeyResolver(endpointIpKeyResolver)
                    )
                )
                .uri("lb://cart-service"))
            
            // SECURED ROUTES - Orders Service (Restrictive due to business impact)
            .route("advanced-orders", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "order-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "orders-restrictive")
                    // Use orders-specific user rate limiter (more restrictive)
                    .requestRateLimiter(c -> c
                        .setRateLimiter(ordersUserRateLimiter)
                        .setKeyResolver(endpointUserKeyResolver)
                    )
                    // Add secondary IP-based rate limiting for orders
                    .requestRateLimiter(c -> c
                        .setRateLimiter(ordersIpRateLimiter)
                        .setKeyResolver(endpointIpKeyResolver)
                    )
                )
                .uri("lb://order-service"))
            
            // SECURED ROUTES - Payments Service (Most restrictive due to financial operations)
            .route("advanced-payments", r -> r
                .path("/api/payments/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Secured-Route", "true")
                    .addRequestHeader("X-Service-Name", "payment-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "payments-strict")
                    // Use payments-specific user rate limiter (most restrictive)
                    .requestRateLimiter(c -> c
                        .setRateLimiter(paymentsUserRateLimiter)
                        .setKeyResolver(endpointUserKeyResolver)
                    )
                    // Add secondary IP-based rate limiting for payments
                    .requestRateLimiter(c -> c
                        .setRateLimiter(paymentsIpRateLimiter)
                        .setKeyResolver(endpointIpKeyResolver)
                    )
                )
                .uri("lb://payment-service"))
            
            // ADMIN ROUTES - Use orders-level restrictions for admin operations
            .route("advanced-admin", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Admin-Route", "true")
                    .addRequestHeader("X-Service-Name", "admin-service")
                    .addRequestHeader("X-Rate-Limit-Policy", "admin-restrictive")
                    // Use orders-level rate limiting for admin operations
                    .requestRateLimiter(c -> c
                        .setRateLimiter(ordersUserRateLimiter)
                        .setKeyResolver(endpointUserKeyResolver)
                    )
                )
                .uri("lb://admin-service"))
            
            // FALLBACK ROUTES - Use basic rate limiting for unmatched paths
            .route("fallback-secured", r -> r
                .path("/api/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Fallback-Route", "true")
                    .addRequestHeader("X-Rate-Limit-Policy", "default-fallback")
                    // Use basic user rate limiter for unmatched secured routes
                    .requestRateLimiter(c -> c
                        .setRateLimiter(userRateLimiter)
                        .setKeyResolver(userKeyResolver)
                    )
                )
                .uri("lb://default-service"))
            
            .build();
    }

    /**
     * Get rate limiting policy summary for monitoring and debugging.
     */
    public String getRateLimitingPolicySummary() {
        return String.format(
            "Advanced Rate Limiting Policies Active:\n" +
            "- Products: %d req/min (Public, Most Permissive)\n" +
            "- Cart: %d/%d req/min user/IP (Secured, Moderate)\n" +
            "- Orders: %d/%d req/min user/IP (Secured, Restrictive)\n" +
            "- Payments: %d/%d req/min user/IP (Secured, Most Restrictive)\n" +
            "- Features: Circuit Breakers, Adaptive Thresholds, Endpoint-Aware Keys",
            PRODUCTS_IP_RATE_LIMIT_REQUESTS,
            CART_USER_RATE_LIMIT_REQUESTS, CART_IP_RATE_LIMIT_REQUESTS,
            ORDERS_USER_RATE_LIMIT_REQUESTS, ORDERS_IP_RATE_LIMIT_REQUESTS,
            PAYMENTS_USER_RATE_LIMIT_REQUESTS, PAYMENTS_IP_RATE_LIMIT_REQUESTS
        );
    }
}
