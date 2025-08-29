package com.mambogo.order.client;

import com.mambogo.order.dto.InventoryReservationRequest;
import com.mambogo.order.dto.InventoryReservationResponse;
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
public class InventoryClient {
    private static final Logger logger = LoggerFactory.getLogger(InventoryClient.class);
    
    private final WebClient webClient;
    private final String productServiceUrl;

    public InventoryClient(WebClient webClient, 
                          @Value("${product.service.url:http://product-service:8082}") String productServiceUrl) {
        this.webClient = webClient;
        this.productServiceUrl = productServiceUrl;
    }

    @TimeLimiter(name = "outbound")
    @Retry(name = "outbound")
    @CircuitBreaker(name = "outbound", fallbackMethod = "reserveInventoryFallback")
    public CompletableFuture<InventoryReservationResponse> reserveInventory(InventoryReservationRequest request) {
        logger.info("Reserving inventory for order: {}", request.getOrderId());
        
        return webClient.post()
                .uri(productServiceUrl + "/inventory/reserve")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InventoryReservationResponse.class)
                .doOnSuccess(response -> logger.info("Inventory reserved successfully for order: {}", request.getOrderId()))
                .doOnError(error -> logger.error("Inventory reservation failed for order: {}", request.getOrderId(), error))
                .toFuture();
    }

    public CompletableFuture<InventoryReservationResponse> reserveInventoryFallback(InventoryReservationRequest request, Throwable ex) {
        logger.warn("Inventory service unavailable, using fallback for order: {}. Error: {}", 
                   request.getOrderId(), ex.getMessage());
        
        // Return a pending reservation response
        InventoryReservationResponse fallbackResponse = new InventoryReservationResponse();
        fallbackResponse.setReservationId(java.util.UUID.randomUUID());
        fallbackResponse.setOrderId(request.getOrderId());
        fallbackResponse.setStatus("RESERVATION_PENDING");
        fallbackResponse.setCreatedAt(java.time.LocalDateTime.now());
        fallbackResponse.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(30));
        
        // Create fallback items with pending status
        java.util.List<InventoryReservationResponse.ReservationItemResponse> fallbackItems = 
            request.getItems().stream()
                .map(item -> new InventoryReservationResponse.ReservationItemResponse(
                    item.getProductId(), 
                    "Unknown Product", 
                    item.getQuantity(), 
                    false))
                .collect(java.util.stream.Collectors.toList());
        
        fallbackResponse.setItems(fallbackItems);
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
}
