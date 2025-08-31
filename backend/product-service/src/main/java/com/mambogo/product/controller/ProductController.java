package com.mambogo.product.controller;

import com.mambogo.product.config.JwtTokenExtractor;
import com.mambogo.product.config.ScopeValidator;
import com.mambogo.product.dto.CreateProductRequest;
import com.mambogo.product.dto.UpdateProductRequest;
import com.mambogo.product.validation.ValidUUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final JwtTokenExtractor jwtTokenExtractor;
    private final ScopeValidator scopeValidator;

    public ProductController(JwtTokenExtractor jwtTokenExtractor, ScopeValidator scopeValidator) {
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.scopeValidator = scopeValidator;
    }

    /**
     * Public endpoint - no authentication required
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Public products endpoint");
        response.put("products", java.util.List.of("Product 1", "Product 2", "Product 3"));
        return ResponseEntity.ok(response);
    }

    /**
     * Product catalog endpoint - requires product:read scope
     */
    @GetMapping
    @PreAuthorize("hasScope('product:read')")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Page size cannot exceed 100") Integer size,
            @RequestParam(required = false) @Pattern(regexp = "^[a-zA-Z0-9\\s-_]{0,100}$", message = "Invalid search query format") String search,
            @RequestParam(required = false) @ValidUUID(message = "Invalid category ID format") UUID categoryId,
            @RequestParam(defaultValue = "name") @Pattern(regexp = "^(name|price|createdAt|updatedAt)$", message = "Invalid sort field") String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'") String sortDirection) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product catalog");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userScopes", scopeValidator.getUserScopes());
        response.put("pagination", Map.of(
            "page", page,
            "size", size,
            "totalElements", 3,
            "totalPages", 1
        ));
        response.put("filters", Map.of(
            "search", search != null ? search : "",
            "categoryId", categoryId != null ? categoryId : "",
            "sortBy", sortBy,
            "sortDirection", sortDirection
        ));
        response.put("products", java.util.List.of(
            Map.of("id", UUID.randomUUID(), "name", "Laptop", "price", 999.99),
            Map.of("id", UUID.randomUUID(), "name", "Mouse", "price", 29.99),
            Map.of("id", UUID.randomUUID(), "name", "Keyboard", "price", 79.99)
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * Product details endpoint - requires product:read scope
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasScope('product:read')")
    public ResponseEntity<Map<String, Object>> getProductDetails(
            @PathVariable @ValidUUID(allowNull = false, message = "Invalid product ID format") UUID productId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product details");
        response.put("productId", productId);
        response.put("product", Map.of(
            "id", productId,
            "name", "Product " + productId,
            "price", 99.99,
            "description", "Description for product " + productId
        ));
        response.put("hasProductReadScope", scopeValidator.hasProductReadScope());
        return ResponseEntity.ok(response);
    }

    /**
     * Create product endpoint - requires admin:all scope
     */
    @PostMapping
    @PreAuthorize("hasScope('admin:all')")
    public ResponseEntity<Map<String, Object>> createProduct(
            @Valid @RequestBody CreateProductRequest productRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product created successfully");
        response.put("product", productRequest);
        response.put("createdBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("hasAdminScope", scopeValidator.hasAdminAllScope());
        return ResponseEntity.ok(response);
    }

    /**
     * Update product endpoint - requires admin:all scope
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasScope('admin:all')")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable @ValidUUID(allowNull = false, message = "Invalid product ID format") UUID productId,
            @Valid @RequestBody UpdateProductRequest productRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product updated successfully");
        response.put("productId", productId);
        response.put("product", productRequest);
        response.put("updatedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete product endpoint - requires admin:all scope
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasScope('admin:all')")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable @ValidUUID(allowNull = false, message = "Invalid product ID format") UUID productId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product deleted successfully");
        response.put("productId", productId);
        response.put("deletedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        return ResponseEntity.ok(response);
    }

    /**
     * User info endpoint - shows JWT token and scope information
     */
    @GetMapping("/user-info")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("email", jwtTokenExtractor.getEmail().orElse("unknown"));
        response.put("fullName", jwtTokenExtractor.getFullName().orElse("unknown"));
        response.put("userRoles", jwtTokenExtractor.getUserRoles());
        response.put("userScopes", scopeValidator.getUserScopes());
        response.put("isAdmin", jwtTokenExtractor.isAdmin());
        response.put("isUser", jwtTokenExtractor.isUser());
        response.put("hasProductReadScope", scopeValidator.hasProductReadScope());
        response.put("hasCartManageScope", scopeValidator.hasCartManageScope());
        response.put("hasOrderWriteScope", scopeValidator.hasOrderWriteScope());
        response.put("hasPaymentProcessScope", scopeValidator.hasPaymentProcessScope());
        response.put("hasAdminAllScope", scopeValidator.hasAdminAllScope());
        return ResponseEntity.ok(response);
    }
}
