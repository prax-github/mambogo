package com.mambogo.order.client;

import com.mambogo.order.dto.PaymentRequest;
import com.mambogo.order.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentClientTest {

    @Test
    void processPaymentFallback_ReturnsValidResponse() throws ExecutionException, InterruptedException {
        // Given
        PaymentClient paymentClient = new PaymentClient(null, "http://payment-service:8085");
        PaymentRequest request = new PaymentRequest(
            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "CREDIT_CARD", "USD"
        );
        RuntimeException exception = new RuntimeException("Service unavailable");

        // When
        CompletableFuture<PaymentResponse> future = paymentClient.processPaymentFallback(request, exception);
        PaymentResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("PAYMENT_DEFERRED", result.getStatus());
        assertEquals(request.getOrderId(), result.getOrderId());
        assertEquals(request.getUserId(), result.getUserId());
        assertEquals(request.getAmount(), result.getAmount());
        assertEquals(request.getPaymentMethod(), result.getPaymentMethod());
        assertEquals(request.getCurrency(), result.getCurrency());
        assertTrue(result.getTransactionId().startsWith("FALLBACK-"));
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void processPaymentFallback_HandlesNullException() throws ExecutionException, InterruptedException {
        // Given
        PaymentClient paymentClient = new PaymentClient(null, "http://payment-service:8085");
        PaymentRequest request = new PaymentRequest(
            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "CREDIT_CARD", "USD"
        );

        // When
        CompletableFuture<PaymentResponse> future = paymentClient.processPaymentFallback(request, null);
        PaymentResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("PAYMENT_DEFERRED", result.getStatus());
        assertTrue(result.getTransactionId().startsWith("FALLBACK-"));
    }
}
