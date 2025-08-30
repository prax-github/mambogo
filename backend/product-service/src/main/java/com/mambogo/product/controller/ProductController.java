package com.mambogo.product.controller;

import com.mambogo.product.config.JwtTokenExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final JwtTokenExtractor jwtTokenExtractor;

    public ProductController(JwtTokenExtractor jwtTokenExtractor) {
        this.jwtTokenExtractor = jwtTokenExtractor;
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
     * User endpoint - requires USER role
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User products endpoint");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userRoles", jwtTokenExtractor.getUserRoles());
        response.put("products", java.util.List.of("User Product 1", "User Product 2"));
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint - requires ADMIN role
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminProducts() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin products endpoint");
        response.put("userId", jwtTokenExtractor.getUserId().orElse("unknown"));
        response.put("username", jwtTokenExtractor.getUsername().orElse("unknown"));
        response.put("userRoles", jwtTokenExtractor.getUserRoles());
        response.put("products", java.util.List.of("Admin Product 1", "Admin Product 2", "Admin Product 3"));
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
