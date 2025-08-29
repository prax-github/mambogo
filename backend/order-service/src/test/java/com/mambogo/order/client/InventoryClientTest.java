package com.mambogo.order.client;

import com.mambogo.order.dto.InventoryReservationRequest;
import com.mambogo.order.dto.InventoryReservationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InventoryClientTest {

    @Test
    void reserveInventoryFallback_ReturnsValidResponse() throws ExecutionException, InterruptedException {
        // Given
        InventoryClient inventoryClient = new InventoryClient(null, "http://product-service:8082");
        InventoryReservationRequest request = new InventoryReservationRequest(
            UUID.randomUUID(),
            Arrays.asList(
                new InventoryReservationRequest.ReservationItem(UUID.randomUUID(), 2),
                new InventoryReservationRequest.ReservationItem(UUID.randomUUID(), 1)
            )
        );
        RuntimeException exception = new RuntimeException("Service unavailable");

        // When
        CompletableFuture<InventoryReservationResponse> future = inventoryClient.reserveInventoryFallback(request, exception);
        InventoryReservationResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("RESERVATION_PENDING", result.getStatus());
        assertEquals(request.getOrderId(), result.getOrderId());
        assertNotNull(result.getReservationId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getExpiresAt());
        assertEquals(2, result.getItems().size());
        
        // Check that all items are marked as unavailable
        result.getItems().forEach(item -> {
            assertFalse(item.isAvailable());
            assertEquals("Unknown Product", item.getProductName());
        });
    }

    @Test
    void reserveInventoryFallback_HandlesNullException() throws ExecutionException, InterruptedException {
        // Given
        InventoryClient inventoryClient = new InventoryClient(null, "http://product-service:8082");
        InventoryReservationRequest request = new InventoryReservationRequest(
            UUID.randomUUID(),
            Arrays.asList(
                new InventoryReservationRequest.ReservationItem(UUID.randomUUID(), 1)
            )
        );

        // When
        CompletableFuture<InventoryReservationResponse> future = inventoryClient.reserveInventoryFallback(request, null);
        InventoryReservationResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("RESERVATION_PENDING", result.getStatus());
        assertEquals(1, result.getItems().size());
        assertFalse(result.getItems().get(0).isAvailable());
    }

    @Test
    void reserveInventoryFallback_HandlesEmptyItems() throws ExecutionException, InterruptedException {
        // Given
        InventoryClient inventoryClient = new InventoryClient(null, "http://product-service:8082");
        InventoryReservationRequest request = new InventoryReservationRequest(
            UUID.randomUUID(),
            Arrays.asList()
        );
        RuntimeException exception = new RuntimeException("Service unavailable");

        // When
        CompletableFuture<InventoryReservationResponse> future = inventoryClient.reserveInventoryFallback(request, exception);
        InventoryReservationResponse result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("RESERVATION_PENDING", result.getStatus());
        assertEquals(0, result.getItems().size());
    }
}
