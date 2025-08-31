package com.mambogo.gateway.filter;

import com.mambogo.gateway.sanitization.*;
import com.mambogo.gateway.metrics.SanitizationMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced input sanitization filter that provides comprehensive protection against
 * injection attacks, XSS, and other security threats at the gateway level.
 * 
 * This filter complements the service-level validation from SEC-07 by providing:
 * - Centralized input sanitization before requests reach microservices
 * - Real-time threat detection and blocking
 * - Endpoint-specific sanitization policies
 * - Comprehensive monitoring and metrics
 * - Performance-optimized sanitization with caching
 * 
 * @author Prashant Sinha
 * @since SEC-11 Implementation
 */
@Component
public class InputSanitizationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(InputSanitizationFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    private final InputSanitizationProperties properties;
    private final SanitizationPolicyManager policyManager;
    private final ThreatDetectionEngine threatDetectionEngine;
    private final SanitizationMetricsCollector metricsCollector;
    private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

    public InputSanitizationFilter(InputSanitizationProperties properties,
                                 SanitizationPolicyManager policyManager,
                                 ThreatDetectionEngine threatDetectionEngine,
                                 SanitizationMetricsCollector metricsCollector) {
        this.properties = properties;
        this.policyManager = policyManager;
        this.threatDetectionEngine = threatDetectionEngine;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        long startTime = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String origin = getClientOrigin(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String method = request.getMethod().name();
        String userId = request.getHeaders().getFirst("X-User-Id");
        boolean authenticated = "true".equals(request.getHeaders().getFirst("X-Authenticated"));

        // Check if origin is blocked due to excessive violations
        if (threatDetectionEngine.isOriginBlocked(origin)) {
            securityLogger.warn("BLOCKED REQUEST - Origin {} is blocked due to excessive security violations", origin);
            metricsCollector.recordBlockedRequest(origin, "ORIGIN_BLOCKED", path);
            return handleSecurityViolation(exchange, "Origin blocked due to security violations", "ORIGIN_BLOCKED");
        }

        // Get sanitization policy for this endpoint
        SanitizationPolicyManager.SanitizationPolicy policy = policyManager.getPolicyForEndpoint(path);
        
        // Create threat analysis context
        ThreatAnalysisContext context = ThreatAnalysisContext.builder()
            .endpoint(extractEndpoint(path))
            .origin(origin)
            .userAgent(userAgent)
            .method(method)
            .path(path)
            .userId(userId)
            .authenticated(authenticated)
            .build();

        // Record sanitization attempt
        metricsCollector.recordSanitizationAttempt(context.getEndpoint(), origin, policy.getName());

        try {
            // Sanitize query parameters
            ServerHttpRequest sanitizedRequest = sanitizeQueryParameters(request, policy, context);
            
            // Sanitize headers
            sanitizedRequest = sanitizeHeaders(sanitizedRequest, policy, context);
            
            // For requests with body content, sanitize the body
            if (hasRequestBody(request)) {
                return sanitizeRequestBody(exchange.mutate().request(sanitizedRequest).build(), 
                                         chain, policy, context, startTime);
            } else {
                // No body to sanitize, continue with sanitized request
                recordSanitizationSuccess(context, startTime, 0);
                return chain.filter(exchange.mutate().request(sanitizedRequest).build());
            }
            
        } catch (Exception e) {
            logger.error("Error during input sanitization for path {}: {}", path, e.getMessage(), e);
            metricsCollector.recordSanitizationError(context.getEndpoint(), origin, "SANITIZATION_ERROR");
            
            // Continue with original request if sanitization fails (fail-open for availability)
            return chain.filter(exchange);
        }
    }

    private ServerHttpRequest sanitizeQueryParameters(ServerHttpRequest request, 
                                                    SanitizationPolicyManager.SanitizationPolicy policy,
                                                    ThreatAnalysisContext context) {
        if (!policy.isSanitizeQueryParams()) {
            return request;
        }

        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (queryParams.isEmpty()) {
            return request;
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(request.getURI()).replaceQuery(null);
        boolean hasThreats = false;

        for (String paramName : queryParams.keySet()) {
            for (String paramValue : queryParams.get(paramName)) {
                if (paramValue != null) {
                    // Analyze for threats
                    ThreatAnalysisResult threatResult = threatDetectionEngine.analyzeInput(paramValue, context);
                    
                    if (threatResult.isThreatDetected()) {
                        hasThreats = true;
                        securityLogger.warn("THREAT DETECTED in query parameter '{}': {} (Score: {})", 
                            paramName, threatResult.getThreatSummary(), threatResult.getThreatScore());
                        
                        if (properties.isBlockSuspiciousRequests() && 
                            threatResult.getThreatScore() >= properties.getThreatDetection().getThreatScoreThreshold()) {
                            throw new SecurityException("High-risk threat detected in query parameters");
                        }
                    }

                    // Sanitize the parameter value
                    String sanitizedValue = policyManager.sanitizeInput(paramValue, policy, 
                        SanitizationPolicyManager.InputType.QUERY_PARAMETER);
                    
                    uriBuilder.queryParam(paramName, sanitizedValue);
                }
            }
        }

        if (hasThreats) {
            metricsCollector.recordThreatDetected(context.getEndpoint(), context.getOrigin(), "QUERY_PARAMETER");
        }

        URI sanitizedUri = uriBuilder.build().toUri();
        return request.mutate().uri(sanitizedUri).build();
    }

    private ServerHttpRequest sanitizeHeaders(ServerHttpRequest request,
                                            SanitizationPolicyManager.SanitizationPolicy policy,
                                            ThreatAnalysisContext context) {
        if (!policy.isSanitizeHeaders()) {
            return request;
        }

        ServerHttpRequest.Builder requestBuilder = request.mutate();
        boolean hasThreats = false;

        // Sanitize specific headers that may contain user input
        String[] headersToSanitize = {"User-Agent", "Referer", "X-Forwarded-For", "X-Real-IP"};
        
        for (String headerName : headersToSanitize) {
            String headerValue = request.getHeaders().getFirst(headerName);
            if (headerValue != null) {
                // Analyze for threats
                ThreatAnalysisResult threatResult = threatDetectionEngine.analyzeInput(headerValue, context);
                
                if (threatResult.isThreatDetected()) {
                    hasThreats = true;
                    securityLogger.warn("THREAT DETECTED in header '{}': {} (Score: {})", 
                        headerName, threatResult.getThreatSummary(), threatResult.getThreatScore());
                }

                // Sanitize the header value
                String sanitizedValue = policyManager.sanitizeInput(headerValue, policy, 
                    SanitizationPolicyManager.InputType.HEADER_VALUE);
                
                requestBuilder.header(headerName, sanitizedValue);
            }
        }

        if (hasThreats) {
            metricsCollector.recordThreatDetected(context.getEndpoint(), context.getOrigin(), "HEADER");
        }

        return requestBuilder.build();
    }

    private Mono<Void> sanitizeRequestBody(ServerWebExchange exchange, GatewayFilterChain chain,
                                         SanitizationPolicyManager.SanitizationPolicy policy,
                                         ThreatAnalysisContext context, long startTime) {
        if (!policy.isSanitizeRequestBody()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        
        return request.getBody()
            .collectList()
            .map(dataBuffers -> {
                // Convert body to string
                StringBuilder bodyBuilder = new StringBuilder();
                dataBuffers.forEach(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    bodyBuilder.append(new String(bytes, StandardCharsets.UTF_8));
                });
                
                return bodyBuilder.toString();
            })
            .flatMap(body -> {
                try {
                    // Analyze body for threats
                    ThreatAnalysisResult threatResult = threatDetectionEngine.analyzeInput(body, context);
                    
                    if (threatResult.isThreatDetected()) {
                        securityLogger.warn("THREAT DETECTED in request body: {} (Score: {})", 
                            threatResult.getThreatSummary(), threatResult.getThreatScore());
                        
                        metricsCollector.recordThreatDetected(context.getEndpoint(), context.getOrigin(), "REQUEST_BODY");
                        
                        if (properties.isBlockSuspiciousRequests() && 
                            threatResult.getThreatScore() >= properties.getThreatDetection().getThreatScoreThreshold()) {
                            return handleSecurityViolation(exchange, "High-risk threat detected in request body", "BODY_THREAT");
                        }
                    }

                    // Sanitize the body
                    String sanitizedBody = policyManager.sanitizeInput(body, policy, 
                        SanitizationPolicyManager.InputType.REQUEST_BODY);
                    
                    // Create new request with sanitized body
                    DataBuffer buffer = dataBufferFactory.wrap(sanitizedBody.getBytes(StandardCharsets.UTF_8));
                    Flux<DataBuffer> bodyFlux = Flux.just(buffer);
                    
                    ServerHttpRequest sanitizedRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return bodyFlux;
                        }
                    };
                    
                    recordSanitizationSuccess(context, startTime, body.length());
                    return chain.filter(exchange.mutate().request(sanitizedRequest).build());
                    
                } catch (Exception e) {
                    logger.error("Error sanitizing request body: {}", e.getMessage(), e);
                    metricsCollector.recordSanitizationError(context.getEndpoint(), context.getOrigin(), "BODY_SANITIZATION_ERROR");
                    
                    // Continue with original request if sanitization fails
                    return chain.filter(exchange);
                }
            });
    }

    private void recordSanitizationSuccess(ThreatAnalysisContext context, long startTime, int bodySize) {
        long duration = System.nanoTime() - startTime;
        
        metricsCollector.recordSanitizationSuccess(context.getEndpoint(), context.getOrigin());
        metricsCollector.recordSanitizationDuration(context.getEndpoint(), context.getOrigin(), duration);
        
        if (bodySize > 0) {
            metricsCollector.recordRequestBodySize(context.getEndpoint(), context.getOrigin(), bodySize);
        }
        
        // Async metrics collection for performance
        if (properties.isEnableAsyncProcessing()) {
            CompletableFuture.runAsync(() -> {
                metricsCollector.recordSanitizationProcessed(context.getEndpoint(), context.getOrigin(), 
                    context.getMethod(), context.isAuthenticated());
            });
        }
    }

    private Mono<Void> handleSecurityViolation(ServerWebExchange exchange, String message, String violationType) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-Security-Violation", violationType);
        
        String jsonResponse = String.format(
            "{\"error\":\"Security Violation\",\"message\":\"%s\",\"code\":\"%s\",\"timestamp\":\"%s\"}",
            message, violationType, Instant.now().toString()
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private boolean hasRequestBody(ServerHttpRequest request) {
        String method = request.getMethod().name();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String getClientOrigin(ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst("Origin");
        if (origin != null) {
            return origin;
        }
        
        String referer = request.getHeaders().getFirst("Referer");
        if (referer != null) {
            try {
                return new URI(referer).getHost();
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    private String extractEndpoint(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        
        String[] segments = path.split("/");
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2];
        }
        
        return "unknown";
    }

    @Override
    public int getOrder() {
        // Run after CORS (-200) and before rate limiting (-100)
        // This ensures we sanitize inputs early but after basic request validation
        return -150;
    }
}
