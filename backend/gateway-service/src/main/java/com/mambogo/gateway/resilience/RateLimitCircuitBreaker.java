package com.mambogo.gateway.resilience;

import com.mambogo.gateway.metrics.AdvancedRateLimitMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mambogo.gateway.config.RateLimitConstants.*;

/**
 * Circuit breaker implementation for rate limiting.
 * Provides automatic failure detection and recovery for rate limiting policies.
 * Integrates with adaptive rate limiting to provide graceful degradation.
 * 
 * @author Prashant Sinha
 * @since SEC-10 Implementation
 */
@Component
public class RateLimitCircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitCircuitBreaker.class);

    private final AdvancedRateLimitMetrics metrics;
    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    public RateLimitCircuitBreaker(AdvancedRateLimitMetrics metrics) {
        this.metrics = metrics;
        logger.info("Rate limit circuit breaker initialized");
    }

    /**
     * Check if circuit breaker allows the request for a given endpoint.
     */
    public boolean allowRequest(String endpoint) {
        CircuitBreakerState state = circuitBreakers.computeIfAbsent(endpoint, 
            k -> new CircuitBreakerState(endpoint));
        
        return state.allowRequest();
    }

    /**
     * Record a successful rate limit check.
     */
    public void recordSuccess(String endpoint) {
        CircuitBreakerState state = circuitBreakers.get(endpoint);
        if (state != null) {
            state.recordSuccess();
        }
    }

    /**
     * Record a failed rate limit check (rate limit exceeded).
     */
    public void recordFailure(String endpoint) {
        CircuitBreakerState state = circuitBreakers.computeIfAbsent(endpoint, 
            k -> new CircuitBreakerState(endpoint));
        
        state.recordFailure();
    }

    /**
     * Get circuit breaker state for monitoring.
     */
    public String getCircuitBreakerState(String endpoint) {
        CircuitBreakerState state = circuitBreakers.get(endpoint);
        return state != null ? state.getCurrentState().name() : "CLOSED";
    }

    /**
     * Get failure rate for an endpoint.
     */
    public double getFailureRate(String endpoint) {
        CircuitBreakerState state = circuitBreakers.get(endpoint);
        return state != null ? state.getFailureRate() : 0.0;
    }

    /**
     * Force circuit breaker state change (for testing/admin operations).
     */
    public void forceState(String endpoint, CircuitState newState) {
        CircuitBreakerState state = circuitBreakers.computeIfAbsent(endpoint, 
            k -> new CircuitBreakerState(endpoint));
        
        state.forceState(newState);
        logger.warn("Circuit breaker state forced: endpoint={}, state={}", endpoint, newState);
    }

    /**
     * Get all circuit breaker states for monitoring dashboard.
     */
    public ConcurrentHashMap<String, String> getAllCircuitBreakerStates() {
        ConcurrentHashMap<String, String> states = new ConcurrentHashMap<>();
        circuitBreakers.forEach((endpoint, state) -> 
            states.put(endpoint, state.getCurrentState().name()));
        return states;
    }

    /**
     * Circuit breaker states.
     */
    public enum CircuitState {
        CLOSED,     // Normal operation
        OPEN,       // Blocking requests due to failures
        HALF_OPEN   // Testing if service has recovered
    }

    /**
     * Circuit breaker state management for a single endpoint.
     */
    private class CircuitBreakerState {
        private final String endpoint;
        private volatile CircuitState currentState = CircuitState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile Instant lastFailureTime = Instant.now();
        @SuppressWarnings("unused") // Used for monitoring and debugging
        private volatile Instant lastStateChange = Instant.now();

        public CircuitBreakerState(String endpoint) {
            this.endpoint = endpoint;
        }

        public boolean allowRequest() {
            switch (currentState) {
                case CLOSED:
                    return true;
                    
                case OPEN:
                    // Check if recovery timeout has passed
                    if (Instant.now().isAfter(lastFailureTime.plusSeconds(CIRCUIT_BREAKER_RECOVERY_TIMEOUT_SECONDS))) {
                        logger.info("Circuit breaker recovery timeout passed for endpoint: {}", endpoint);
                        changeState(CircuitState.HALF_OPEN);
                        return true;
                    }
                    return false;
                    
                case HALF_OPEN:
                    // Allow limited requests to test recovery
                    return true;
                    
                default:
                    return false;
            }
        }

        public void recordSuccess() {
            successCount.incrementAndGet();
            
            if (currentState == CircuitState.HALF_OPEN) {
                // If in half-open state and we get a success, close the circuit
                logger.info("Circuit breaker closing after successful request: endpoint={}", endpoint);
                failureCount.set(0); // Reset failure count
                changeState(CircuitState.CLOSED);
            }
        }

        public void recordFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime = Instant.now();
            
            if (currentState == CircuitState.CLOSED && failures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
                logger.warn("Circuit breaker opening due to failure threshold: endpoint={}, failures={}", 
                           endpoint, failures);
                changeState(CircuitState.OPEN);
            } else if (currentState == CircuitState.HALF_OPEN) {
                logger.warn("Circuit breaker re-opening due to failure in half-open state: endpoint={}", endpoint);
                changeState(CircuitState.OPEN);
            }
        }

        public double getFailureRate() {
            int total = failureCount.get() + successCount.get();
            return total > 0 ? (double) failureCount.get() / total : 0.0;
        }

        public CircuitState getCurrentState() {
            return currentState;
        }

        public void forceState(CircuitState newState) {
            changeState(newState);
        }

        private void changeState(CircuitState newState) {
            CircuitState oldState = currentState;
            currentState = newState;
            lastStateChange = Instant.now();
            
            // Record state change in metrics
            metrics.recordCircuitBreakerState(endpoint, newState.name());
            
            logger.info("Circuit breaker state changed: endpoint={}, from={}, to={}, failureCount={}, successCount={}", 
                       endpoint, oldState, newState, failureCount.get(), successCount.get());
        }
    }
}
