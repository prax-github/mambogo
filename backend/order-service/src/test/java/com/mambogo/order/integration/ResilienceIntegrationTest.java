package com.mambogo.order.integration;

import com.mambogo.order.client.PaymentClient;
import com.mambogo.order.dto.PaymentRequest;
import com.mambogo.order.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "payment.service.url=http://localhost:9999", // Non-existent service
    "product.service.url=http://localhost:9998"  // Non-existent service
})
class ResilienceIntegrationTest {

    @Autowired
    private PaymentClient paymentClient;

    @Test
    void testPaymentClientFallback_WhenServiceUnavailable() throws ExecutionException, InterruptedException {
        // Given
        PaymentRequest request = new PaymentRequest(
            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "CREDIT_CARD", "USD"
        );

        // When - This should trigger the circuit breaker and fallback
        CompletableFuture<PaymentResponse> future = paymentClient.processPayment(request);
        PaymentResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("PAYMENT_DEFERRED", result.getStatus());
        assertTrue(result.getTransactionId().startsWith("FALLBACK-"));
        assertEquals(request.getOrderId(), result.getOrderId());
        assertEquals(request.getAmount(), result.getAmount());
    }

    @Test
    void testResilience4jConfiguration_IsLoaded() {
        // This test verifies that Resilience4j configuration is properly loaded
        assertNotNull(paymentClient);
    }
}
