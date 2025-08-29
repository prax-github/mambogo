package com.mambogo.payment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ExternalPaymentClient {
    private static final Logger logger = LoggerFactory.getLogger(ExternalPaymentClient.class);
    
    private final WebClient webClient;
    private final String pspUrl;

    public ExternalPaymentClient(WebClient webClient, 
                                @Value("${psp.service.url:http://mock-psp:8086}") String pspUrl) {
        this.webClient = webClient;
        this.pspUrl = pspUrl;
    }

    @TimeLimiter(name = "outbound")
    @Retry(name = "outbound")
    @CircuitBreaker(name = "outbound", fallbackMethod = "processPaymentFallback")
    public CompletableFuture<PSPResponse> processPayment(PSPRequest request) {
        logger.info("Processing payment with external PSP for order: {}", request.getOrderId());
        
        return webClient.post()
                .uri(pspUrl + "/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PSPResponse.class)
                .doOnSuccess(response -> logger.info("PSP payment processed successfully for order: {}", request.getOrderId()))
                .doOnError(error -> logger.error("PSP payment processing failed for order: {}", request.getOrderId(), error))
                .toFuture();
    }

    public CompletableFuture<PSPResponse> processPaymentFallback(PSPRequest request, Throwable ex) {
        logger.warn("External PSP unavailable, using fallback for order: {}. Error: {}", 
                   request.getOrderId(), ex.getMessage());
        
        // Return a failed payment response
        PSPResponse fallbackResponse = new PSPResponse();
        fallbackResponse.setTransactionId("FALLBACK-" + System.currentTimeMillis());
        fallbackResponse.setStatus("FAILED");
        fallbackResponse.setMessage("Payment service temporarily unavailable");
        fallbackResponse.setOrderId(request.getOrderId());
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public static class PSPRequest {
        private UUID orderId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String cardNumber;
        private String expiryDate;
        private String cvv;

        // Constructors
        public PSPRequest() {}

        public PSPRequest(UUID orderId, BigDecimal amount, String currency, String paymentMethod,
                         String cardNumber, String expiryDate, String cvv) {
            this.orderId = orderId;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
            this.cardNumber = cardNumber;
            this.expiryDate = expiryDate;
            this.cvv = cvv;
        }

        // Getters and Setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
        public String getCvv() { return cvv; }
        public void setCvv(String cvv) { this.cvv = cvv; }
    }

    public static class PSPResponse {
        private String transactionId;
        private String status;
        private String message;
        private UUID orderId;
        private String authorizationCode;

        // Constructors
        public PSPResponse() {}

        public PSPResponse(String transactionId, String status, String message, UUID orderId, String authorizationCode) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
            this.orderId = orderId;
            this.authorizationCode = authorizationCode;
        }

        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    }
}
