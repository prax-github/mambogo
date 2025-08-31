package com.mambogo.product.dto;

import com.mambogo.product.validation.NoSuspiciousInput;
import com.mambogo.product.validation.ValidUUID;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new product with comprehensive validation.
 */
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 100, message = "Product name must be between 1 and 100 characters")
    @NoSuspiciousInput(message = "Product name contains invalid characters")
    private String name;

    @Size(max = 1000, message = "Product description must not exceed 1000 characters")
    @NoSuspiciousInput(checkCommandInjection = false, checkPathTraversal = false, 
                      message = "Product description contains potentially dangerous content")
    private String description;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Product price must be at least 0.01")
    @DecimalMax(value = "99999.99", message = "Product price must not exceed 99999.99")
    @Digits(integer = 5, fraction = 2, message = "Product price format is invalid")
    private BigDecimal price;

    @NotBlank(message = "Product SKU is required")
    @Size(min = 3, max = 50, message = "Product SKU must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "Product SKU can only contain uppercase letters, numbers, hyphens, and underscores")
    private String sku;

    @NotNull(message = "Product category ID is required")
    @ValidUUID(allowNull = false, message = "Invalid category ID format")
    private UUID categoryId;

    @NotBlank(message = "Product category name is required")
    @Size(min = 1, max = 50, message = "Category name must be between 1 and 50 characters")
    @NoSuspiciousInput(message = "Category name contains invalid characters")
    private String categoryName;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 999999, message = "Stock quantity cannot exceed 999999")
    private Integer stockQuantity = 0;

    @Size(max = 5, message = "Cannot have more than 5 product images")
    private List<@NotBlank @URL(message = "Invalid image URL format") String> imageUrls;

    @Size(max = 20, message = "Cannot have more than 20 product tags")
    private List<@NotBlank @Size(max = 30) @NoSuspiciousInput String> tags;

    @DecimalMin(value = "0.0", message = "Product weight cannot be negative")
    @DecimalMax(value = "9999.99", message = "Product weight cannot exceed 9999.99 kg")
    @Digits(integer = 4, fraction = 2, message = "Product weight format is invalid")
    private BigDecimal weight;

    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    @NoSuspiciousInput(message = "Brand name contains invalid characters")
    private String brand;

    @AssertTrue(message = "Product must have either description or at least one image")
    public boolean isDescriptionOrImageProvided() {
        return (description != null && !description.trim().isEmpty()) || 
               (imageUrls != null && !imageUrls.isEmpty());
    }

    // Constructors
    public CreateProductRequest() {}

    public CreateProductRequest(String name, String description, BigDecimal price, String sku, 
                               UUID categoryId, String categoryName) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public String toString() {
        return "CreateProductRequest{" +
                "name='" + name + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(50, description.length())) + "..." : null) + '\'' +
                ", price=" + price +
                ", sku='" + sku + '\'' +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", stockQuantity=" + stockQuantity +
                ", imageUrls=" + (imageUrls != null ? imageUrls.size() + " images" : "no images") +
                ", tags=" + (tags != null ? tags.size() + " tags" : "no tags") +
                ", weight=" + weight +
                ", brand='" + brand + '\'' +
                '}';
    }
}
