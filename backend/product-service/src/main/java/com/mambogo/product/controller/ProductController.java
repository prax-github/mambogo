package com.mambogo.product.controller;

import com.mambogo.product.config.JwtTokenExtractor;
import com.mambogo.product.config.ScopeValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
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
    public ResponseEntity<Map<String, Object>> getProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product catalog");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userScopes", scopeValidator.getUserScopes());
        response.put("products", java.util.List.of(
            Map.of("id", 1, "name", "Laptop", "price", 999.99),
            Map.of("id", 2, "name", "Mouse", "price", 29.99),
            Map.of("id", 3, "name", "Keyboard", "price", 79.99)
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * Product details endpoint - requires product:read scope
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasScope('product:read')")
    public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable Long productId) {
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
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> productData) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product created successfully");
        response.put("product", productData);
        response.put("createdBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("hasAdminScope", scopeValidator.hasAdminAllScope());
        return ResponseEntity.ok(response);
    }

    /**
     * Update product endpoint - requires admin:all scope
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasScope('admin:all')")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long productId, @RequestBody Map<String, Object> productData) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product updated successfully");
        response.put("productId", productId);
        response.put("product", productData);
        response.put("updatedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete product endpoint - requires admin:all scope
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasScope('admin:all')")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long productId) {
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
