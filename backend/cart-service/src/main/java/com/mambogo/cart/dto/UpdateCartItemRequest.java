package com.mambogo.cart.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating cart item quantity with validation.
 */
public class UpdateCartItemRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 50, message = "Cannot have more than 50 items of the same product")
    private Integer quantity;

    @Size(max = 200, message = "Notes must not exceed 200 characters")
    private String notes;

    // Constructors
    public UpdateCartItemRequest() {}

    public UpdateCartItemRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public UpdateCartItemRequest(Integer quantity, String notes) {
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters and Setters
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @AssertTrue(message = "Quantity of 0 will remove the item from cart")
    public boolean isQuantityValid() {
        return quantity != null && quantity >= 0;
    }

    @Override
    public String toString() {
        return "UpdateCartItemRequest{" +
                "quantity=" + quantity +
                ", notes='" + notes + '\'' +
                '}';
    }
}
