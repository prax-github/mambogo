package com.mambogo.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Custom handler for rate limit exceeded responses
 */
@Component
public class RateLimitExceededHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitExceededHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Handles rate limit exceeded scenarios with custom error response
     */
    public Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, 
                                            String remainingRequests, 
                                            String resetTime) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Set status and headers
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        // Add rate limiting headers
        if (remainingRequests != null) {
            response.getHeaders().add(RATE_LIMIT_REMAINING_HEADER, remainingRequests);
        }
        if (resetTime != null) {
            response.getHeaders().add(RATE_LIMIT_RESET_HEADER, resetTime);
        }
        
        // Create error response body
        Map<String, Object> errorResponse = Map.of(
            "code", RATE_LIMIT_ERROR_CODE,
            "message", RATE_LIMIT_EXCEEDED_MESSAGE,
            "timestamp", Instant.now().toString(),
            "path", exchange.getRequest().getPath().value(),
            "status", HttpStatus.TOO_MANY_REQUESTS.value()
        );
        
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes());
            
            logger.warn("Rate limit exceeded for path: {} from IP: {}", 
                       exchange.getRequest().getPath().value(),
                       getClientIp(exchange));
            
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            logger.error("Error creating rate limit exceeded response", e);
            return response.setComplete();
        }
    }
    
    /**
     * Extracts client IP for logging purposes
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp;
        }
        
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
