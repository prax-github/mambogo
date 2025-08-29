package com.mambogo.order.dto;

import java.util.List;
import java.util.UUID;

public class InventoryReservationRequest {
    private UUID orderId;
    private List<ReservationItem> items;

    // Constructors
    public InventoryReservationRequest() {}

    public InventoryReservationRequest(UUID orderId, List<ReservationItem> items) {
        this.orderId = orderId;
        this.items = items;
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }

    public static class ReservationItem {
        private UUID productId;
        private int quantity;

        public ReservationItem() {}

        public ReservationItem(UUID productId, int quantity) {
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
