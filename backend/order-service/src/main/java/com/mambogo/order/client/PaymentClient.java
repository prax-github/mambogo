package com.mambogo.order.client;

import com.mambogo.order.dto.PaymentRequest;
import com.mambogo.order.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.concurrent.CompletableFuture;

@Component
public class PaymentClient {
    private static final Logger logger = LoggerFactory.getLogger(PaymentClient.class);
    
    private final WebClient webClient;
    private final String paymentServiceUrl;

    public PaymentClient(WebClient webClient, 
                        @Value("${payment.service.url:http://payment-service:8085}") String paymentServiceUrl) {
        this.webClient = webClient;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @TimeLimiter(name = "outbound")
    @Retry(name = "outbound")
    @CircuitBreaker(name = "outbound", fallbackMethod = "processPaymentFallback")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest request) {
        logger.info("Processing payment for order: {}", request.getOrderId());
        
        return webClient.post()
                .uri(paymentServiceUrl + "/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnSuccess(response -> logger.info("Payment processed successfully for order: {}", request.getOrderId()))
                .doOnError(error -> logger.error("Payment processing failed for order: {}", request.getOrderId(), error))
                .toFuture();
    }

    public CompletableFuture<PaymentResponse> processPaymentFallback(PaymentRequest request, Throwable ex) {
        logger.warn("Payment service unavailable, using fallback for order: {}. Error: {}", 
                   request.getOrderId(), ex.getMessage());
        
        // Return a deferred payment response
        PaymentResponse fallbackResponse = new PaymentResponse();
        fallbackResponse.setId(java.util.UUID.randomUUID());
        fallbackResponse.setOrderId(request.getOrderId());
        fallbackResponse.setUserId(request.getUserId());
        fallbackResponse.setAmount(request.getAmount());
        fallbackResponse.setStatus("PAYMENT_DEFERRED");
        fallbackResponse.setPaymentMethod(request.getPaymentMethod());
        fallbackResponse.setCurrency(request.getCurrency());
        fallbackResponse.setTransactionId("FALLBACK-" + System.currentTimeMillis());
        fallbackResponse.setCreatedAt(java.time.LocalDateTime.now());
        fallbackResponse.setUpdatedAt(java.time.LocalDateTime.now());
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
}
