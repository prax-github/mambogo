package com.mambogo.cart.controller;

import com.mambogo.cart.config.JwtTokenExtractor;
import com.mambogo.cart.config.ScopeValidator;
import com.mambogo.cart.dto.AddToCartRequest;
import com.mambogo.cart.dto.UpdateCartItemRequest;
import com.mambogo.cart.validation.ValidUUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

    private final JwtTokenExtractor jwtTokenExtractor;
    private final ScopeValidator scopeValidator;

    public CartController(JwtTokenExtractor jwtTokenExtractor, ScopeValidator scopeValidator) {
        this.jwtTokenExtractor = jwtTokenExtractor;
        this.scopeValidator = scopeValidator;
    }

    /**
     * Get user's cart - requires cart:manage scope
     */
    @GetMapping
    @PreAuthorize("hasScope('cart:manage')")
    public ResponseEntity<Map<String, Object>> getUserCart() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User cart");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userScopes", scopeValidator.getUserScopes());
        response.put("cart", Map.of(
            "id", jwtTokenExtractor.getUserId().orElse("cart-123"),
            "items", java.util.List.of(
                Map.of("productId", 1, "name", "Laptop", "quantity", 1, "price", 999.99),
                Map.of("productId", 2, "name", "Mouse", "quantity", 2, "price", 29.99)
            ),
            "totalItems", 3,
            "totalPrice", 1059.97
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * Add item to cart - requires cart:manage scope
     */
    @PostMapping("/items")
    @PreAuthorize("hasScope('cart:manage')")
    public ResponseEntity<Map<String, Object>> addItemToCart(
            @Valid @RequestBody AddToCartRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item added to cart successfully");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("addedItem", request);
        response.put("hasCartManageScope", scopeValidator.hasCartManageScope());
        return ResponseEntity.ok(response);
    }

    /**
     * Update item quantity in cart - requires cart:manage scope
     */
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasScope('cart:manage')")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable @ValidUUID(allowNull = false, message = "Invalid item ID format") UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cart item updated successfully");
        response.put("itemId", itemId);
        response.put("newQuantity", request.getQuantity());
        response.put("updatedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from cart - requires cart:manage scope
     */
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasScope('cart:manage')")
    public ResponseEntity<Map<String, Object>> removeCartItem(
            @PathVariable @ValidUUID(allowNull = false, message = "Invalid item ID format") UUID itemId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item removed from cart successfully");
        response.put("itemId", itemId);
        response.put("removedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        return ResponseEntity.ok(response);
    }

    /**
     * Clear entire cart - requires cart:manage scope
     */
    @DeleteMapping
    @PreAuthorize("hasScope('cart:manage')")
    public ResponseEntity<Map<String, Object>> clearCart() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cart cleared successfully");
        response.put("clearedBy", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
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
