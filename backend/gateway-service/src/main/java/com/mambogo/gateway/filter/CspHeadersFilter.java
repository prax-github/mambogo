package com.mambogo.gateway.filter;

import com.mambogo.gateway.config.CspPolicyProperties;
import com.mambogo.gateway.csp.CspPolicyManager;
import com.mambogo.gateway.csp.CspValidationResult;
import com.mambogo.gateway.metrics.CspMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Advanced Content Security Policy (CSP) filter that applies comprehensive CSP headers
 * with dynamic policy generation, nonce support, violation monitoring, and performance tracking.
 * 
 * This filter enhances the basic CSP implementation from CorsSecurityFilter with:
 * - Dynamic CSP policy generation based on request context
 * - Environment-specific CSP configurations
 * - Nonce generation for inline scripts/styles
 * - CSP violation monitoring and metrics
 * - Performance monitoring for CSP overhead
 * - CSP policy validation and compliance checking
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@Component
public class CspHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CspHeadersFilter.class);

    private final CspPolicyProperties cspProperties;
    private final CspPolicyManager policyManager;
    private final CspMetricsCollector metricsCollector;

    public CspHeadersFilter(CspPolicyProperties cspProperties,
                           CspPolicyManager policyManager,
                           CspMetricsCollector metricsCollector) {
        this.cspProperties = cspProperties;
        this.policyManager = policyManager;
        this.metricsCollector = metricsCollector;
        
        logger.info("CSP Headers Filter initialized - CSP enabled: {}, Report-only: {}", 
                   cspProperties.isEnabled(), cspProperties.isReportOnly());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!cspProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String requestPath = request.getPath().value();
        String origin = request.getHeaders().getOrigin();
        
        long startTime = System.nanoTime();
        
        try {
            // Generate CSP policy for this request
            String cspPolicy = policyManager.generateCspPolicy(requestPath, origin);
            
            if (cspPolicy != null && !cspPolicy.trim().isEmpty()) {
                // Validate policy before applying
                CspValidationResult validationResult = policyManager.validatePolicy(cspPolicy);
                
                if (!validationResult.isValid()) {
                    logger.warn("Generated CSP policy has validation errors: {}", 
                               validationResult.getSummary());
                    metricsCollector.recordPolicyValidationError(requestPath, origin);
                } else if (validationResult.hasIssues()) {
                    logger.debug("Generated CSP policy has recommendations: {}", 
                                validationResult.getSummary());
                }
                
                // Generate nonce if enabled
                String nonce = null;
                if (cspProperties.isNonceEnabled()) {
                    nonce = policyManager.generateNonce();
                    if (nonce != null) {
                        // Replace nonce placeholder in policy
                        cspPolicy = cspPolicy.replace("{nonce}", nonce);
                        // Add nonce to request attributes for potential frontend use
                        exchange.getAttributes().put("csp-nonce", nonce);
                    }
                }
                
                // Apply CSP header
                String headerName = policyManager.getCspHeaderName();
                response.getHeaders().set(headerName, cspPolicy);
                
                // Log CSP application
                logger.debug("Applied CSP header '{}' for path '{}' from origin '{}': {}", 
                           headerName, requestPath, origin, cspPolicy);
                
                // Record metrics
                metricsCollector.recordCspPolicyApplied(requestPath, origin, headerName);
                
                if (nonce != null) {
                    metricsCollector.recordNonceGenerated(requestPath, origin);
                }
                
                // Record policy validation metrics
                if (cspProperties.isMetricsEnabled()) {
                    CompletableFuture.runAsync(() -> {
                        metricsCollector.recordPolicyValidationResult(
                            requestPath, origin, validationResult.isValid(), 
                            validationResult.getErrors().size(),
                            validationResult.getWarnings().size(),
                            validationResult.getRecommendations().size()
                        );
                    });
                }
            }
            
            // Add additional security headers that complement CSP
            addComplementarySecurityHeaders(response);
            
            return chain.filter(exchange);
            
        } catch (Exception e) {
            logger.error("Error applying CSP headers for path '{}' from origin '{}': {}", 
                        requestPath, origin, e.getMessage(), e);
            
            metricsCollector.recordCspProcessingError(requestPath, origin, e.getClass().getSimpleName());
            
            // Continue with request even if CSP application fails
            return chain.filter(exchange);
            
        } finally {
            // Record performance metrics
            if (cspProperties.isPerformanceMonitoringEnabled()) {
                long endTime = System.nanoTime();
                long durationNanos = endTime - startTime;
                metricsCollector.recordCspProcessingTime(requestPath, origin, durationNanos);
            }
        }
    }

    /**
     * Security headers are now handled by the centralized SecurityHeadersFilter.
     * This method is kept for backward compatibility but no longer adds headers.
     */
    private void addComplementarySecurityHeaders(ServerHttpResponse response) {
        // Security headers are now managed centrally by SecurityHeadersFilter
        // This method is kept for backward compatibility
        logger.debug("Security headers are now managed centrally by SecurityHeadersFilter");
    }





    @Override
    public int getOrder() {
        // Run after CORS filter but before routing
        return -90;
    }
}
