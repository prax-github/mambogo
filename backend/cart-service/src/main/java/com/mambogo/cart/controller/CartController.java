package com.mambogo.cart.controller;

import com.mambogo.cart.config.JwtTokenExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final JwtTokenExtractor jwtTokenExtractor;

    public CartController(JwtTokenExtractor jwtTokenExtractor) {
        this.jwtTokenExtractor = jwtTokenExtractor;
    }

    /**
     * Get user's cart - requires USER role
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserCart() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User cart endpoint");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userRoles", jwtTokenExtractor.getUserRoles());
        response.put("cart", Map.of(
            "items", java.util.List.of("Product 1", "Product 2"),
            "totalItems", 2,
            "totalPrice", 99.99
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * Add item to cart - requires USER role
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> addItemToCart(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Item added to cart");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("item", request.get("item"));
        response.put("quantity", request.get("quantity"));
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * User info endpoint - shows JWT token information
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
        response.put("isAdmin", jwtTokenExtractor.isAdmin());
        response.put("isUser", jwtTokenExtractor.isUser());
        return ResponseEntity.ok(response);
    }
}
