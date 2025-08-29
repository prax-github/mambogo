package com.mambogo.order.dto;

import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {
    private UUID userId;
    private List<OrderItemRequest> items;
    private String shippingAddress;
    private String paymentMethod;

    // Constructors
    public CreateOrderRequest() {}

    public CreateOrderRequest(UUID userId, List<OrderItemRequest> items, String shippingAddress, String paymentMethod) {
        this.userId = userId;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static class OrderItemRequest {
        private UUID productId;
        private int quantity;

        public OrderItemRequest() {}

        public OrderItemRequest(UUID productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
