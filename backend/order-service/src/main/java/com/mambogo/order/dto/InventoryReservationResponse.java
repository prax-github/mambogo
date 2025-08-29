package com.mambogo.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InventoryReservationResponse {
    private UUID reservationId;
    private UUID orderId;
    private String status;
    private List<ReservationItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Constructors
    public InventoryReservationResponse() {}

    public InventoryReservationResponse(UUID reservationId, UUID orderId, String status,
                                       List<ReservationItemResponse> items, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ReservationItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemResponse> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public static class ReservationItemResponse {
        private UUID productId;
        private String productName;
        private int quantity;
        private boolean available;

        public ReservationItemResponse() {}

        public ReservationItemResponse(UUID productId, String productName, int quantity, boolean available) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.available = available;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
