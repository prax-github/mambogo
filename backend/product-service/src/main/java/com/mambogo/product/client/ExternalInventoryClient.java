package com.mambogo.product.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ExternalInventoryClient {
    private static final Logger logger = LoggerFactory.getLogger(ExternalInventoryClient.class);
    
    private final WebClient webClient;
    private final String externalInventoryUrl;

    public ExternalInventoryClient(WebClient webClient, 
                                  @Value("${external.inventory.url:http://external-inventory:8087}") String externalInventoryUrl) {
        this.webClient = webClient;
        this.externalInventoryUrl = externalInventoryUrl;
    }

    @TimeLimiter(name = "outbound")
    @Retry(name = "outbound")
    @CircuitBreaker(name = "outbound", fallbackMethod = "checkAvailabilityFallback")
    public CompletableFuture<InventoryAvailabilityResponse> checkAvailability(List<UUID> productIds) {
        logger.info("Checking inventory availability for products: {}", productIds);
        
        return webClient.post()
                .uri(externalInventoryUrl + "/availability")
                .bodyValue(new AvailabilityRequest(productIds))
                .retrieve()
                .bodyToMono(InventoryAvailabilityResponse.class)
                .doOnSuccess(response -> logger.info("Inventory availability checked successfully for products: {}", productIds))
                .doOnError(error -> logger.error("Inventory availability check failed for products: {}", productIds, error))
                .toFuture();
    }

    public CompletableFuture<InventoryAvailabilityResponse> checkAvailabilityFallback(List<UUID> productIds, Throwable ex) {
        logger.warn("External inventory service unavailable, using fallback for products: {}. Error: {}", 
                   productIds, ex.getMessage());
        
        // Return a response indicating availability is unknown
        InventoryAvailabilityResponse fallbackResponse = new InventoryAvailabilityResponse();
        fallbackResponse.setStatus("UNAVAILABLE");
        fallbackResponse.setMessage("External inventory service temporarily unavailable");
        
        List<InventoryAvailabilityResponse.ProductAvailability> fallbackProducts = 
            productIds.stream()
                .map(productId -> new InventoryAvailabilityResponse.ProductAvailability(
                    productId, 
                    "Unknown", 
                    0, 
                    false))
                .collect(java.util.stream.Collectors.toList());
        
        fallbackResponse.setProducts(fallbackProducts);
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }

    public static class AvailabilityRequest {
        private List<UUID> productIds;

        public AvailabilityRequest() {}

        public AvailabilityRequest(List<UUID> productIds) {
            this.productIds = productIds;
        }

        public List<UUID> getProductIds() { return productIds; }
        public void setProductIds(List<UUID> productIds) { this.productIds = productIds; }
    }

    public static class InventoryAvailabilityResponse {
        private String status;
        private String message;
        private List<ProductAvailability> products;

        public InventoryAvailabilityResponse() {}

        public InventoryAvailabilityResponse(String status, String message, List<ProductAvailability> products) {
            this.status = status;
            this.message = message;
            this.products = products;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<ProductAvailability> getProducts() { return products; }
        public void setProducts(List<ProductAvailability> products) { this.products = products; }

        public static class ProductAvailability {
            private UUID productId;
            private String productName;
            private int availableQuantity;
            private boolean inStock;

            public ProductAvailability() {}

            public ProductAvailability(UUID productId, String productName, int availableQuantity, boolean inStock) {
                this.productId = productId;
                this.productName = productName;
                this.availableQuantity = availableQuantity;
                this.inStock = inStock;
            }

            public UUID getProductId() { return productId; }
            public void setProductId(UUID productId) { this.productId = productId; }
            public String getProductName() { return productName; }
            public void setProductName(String productName) { this.productName = productName; }
            public int getAvailableQuantity() { return availableQuantity; }
            public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
            public boolean isInStock() { return inStock; }
            public void setInStock(boolean inStock) { this.inStock = inStock; }
        }
    }
}
