package com.mambogo.cart.dto;

import com.mambogo.cart.validation.ValidUUID;
import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * Request DTO for adding items to cart with comprehensive validation.
 */
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    @ValidUUID(allowNull = false, message = "Invalid product ID format")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Cannot add more than 50 items of the same product")
    private Integer quantity;

    @Size(max = 200, message = "Notes must not exceed 200 characters")
    private String notes;

    // Constructors
    public AddToCartRequest() {}

    public AddToCartRequest(UUID productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public AddToCartRequest(UUID productId, Integer quantity, String notes) {
        this.productId = productId;
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters and Setters
    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

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

    @Override
    public String toString() {
        return "AddToCartRequest{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                ", notes='" + notes + '\'' +
                '}';
    }
}
