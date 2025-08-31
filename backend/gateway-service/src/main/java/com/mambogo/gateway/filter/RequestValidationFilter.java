package com.mambogo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Global filter for request validation including size limits, content type validation,
 * and basic security checks at the gateway level.
 */
@Component
public class RequestValidationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    // Request size limits
    private static final long MAX_REQUEST_SIZE = 1024 * 1024; // 1MB
    private static final long MAX_HEADER_SIZE = 8192; // 8KB
    private static final int MAX_HEADERS_COUNT = 50;
    private static final int MAX_QUERY_PARAMS = 20;
    private static final int MAX_PATH_LENGTH = 2048;

    // Allowed content types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.TEXT_PLAIN_VALUE
    );

    // Security patterns
    private static final Pattern SUSPICIOUS_PATH_PATTERN = Pattern.compile(
        "(?i)(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c|<script|javascript:|vbscript:|on\\w+\\s*=)"
    );

    private static final Pattern SUSPICIOUS_HEADER_PATTERN = Pattern.compile(
        "(?i)(\\$\\{|%\\{|\\${.*}|%{.*}|<script|javascript:|eval\\(|exec\\()"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        try {
            // 1. Validate request path
            String path = request.getPath().value();
            if (path.length() > MAX_PATH_LENGTH) {
                return handleValidationError(exchange, "Request path too long", "PATH_TOO_LONG");
            }
            
            if (SUSPICIOUS_PATH_PATTERN.matcher(path).find()) {
                securityLogger.warn("Suspicious path detected: {} from IP: {}", 
                    sanitizeForLogging(path), getClientIp(request));
                return handleValidationError(exchange, "Invalid request path", "SUSPICIOUS_PATH");
            }

            // 2. Validate query parameters
            if (request.getQueryParams().size() > MAX_QUERY_PARAMS) {
                return handleValidationError(exchange, "Too many query parameters", "QUERY_PARAMS_LIMIT_EXCEEDED");
            }

            // 3. Validate headers
            if (request.getHeaders().size() > MAX_HEADERS_COUNT) {
                return handleValidationError(exchange, "Too many headers", "HEADERS_LIMIT_EXCEEDED");
            }

            // Check for suspicious header values
            for (String headerName : request.getHeaders().keySet()) {
                List<String> headerValues = request.getHeaders().get(headerName);
                if (headerValues != null) {
                    for (String headerValue : headerValues) {
                        if (headerValue.length() > MAX_HEADER_SIZE) {
                            return handleValidationError(exchange, "Header value too large", "HEADER_TOO_LARGE");
                        }
                        
                        if (SUSPICIOUS_HEADER_PATTERN.matcher(headerValue).find()) {
                            securityLogger.warn("Suspicious header detected: {}={} from IP: {}", 
                                headerName, sanitizeForLogging(headerValue), getClientIp(request));
                            return handleValidationError(exchange, "Invalid header value", "SUSPICIOUS_HEADER");
                        }
                    }
                }
            }

            // 4. Validate content type for requests with body
            String method = request.getMethod().name();
            if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) && 
                request.getHeaders().getContentType() != null) {
                
                String contentType = request.getHeaders().getContentType().toString();
                boolean isAllowedContentType = ALLOWED_CONTENT_TYPES.stream()
                    .anyMatch(allowed -> contentType.startsWith(allowed));
                
                if (!isAllowedContentType) {
                    logger.warn("Unsupported content type: {} from IP: {}", contentType, getClientIp(request));
                    return handleValidationError(exchange, "Unsupported content type", "INVALID_CONTENT_TYPE");
                }
            }

            // 5. Validate request size
            Long contentLength = request.getHeaders().getContentLength();
            if (contentLength != null && contentLength > MAX_REQUEST_SIZE) {
                logger.warn("Request size too large: {} bytes from IP: {}", contentLength, getClientIp(request));
                return handleValidationError(exchange, "Request size too large", "REQUEST_TOO_LARGE");
            }

            // 6. Log successful validation for monitoring
            if (logger.isDebugEnabled()) {
                logger.debug("Request validation passed for {} {} from IP: {}", 
                    method, path, getClientIp(request));
            }

            // Continue to next filter
            return chain.filter(exchange);

        } catch (Exception e) {
            logger.error("Error during request validation: {}", e.getMessage(), e);
            return handleValidationError(exchange, "Request validation failed", "VALIDATION_ERROR");
        }
    }

    /**
     * Handle validation errors with consistent response format
     */
    private Mono<Void> handleValidationError(ServerWebExchange exchange, String message, String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String errorResponse = String.format(
            "{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"path\":\"%s\",\"service\":\"gateway-service\"}",
            errorCode,
            message,
            Instant.now().toString(),
            exchange.getRequest().getPath().value()
        );

        DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Sanitize input for logging to prevent log injection
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        
        // Remove newlines and control characters
        String sanitized = input.replaceAll("[\\r\\n\\t]", " ");
        
        // Truncate if too long
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        
        return sanitized;
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain, but after CORS
        return -100;
    }
}
