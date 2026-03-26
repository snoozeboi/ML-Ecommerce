package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.AddToCartRequest;
import com.shop.ecommerce.dto.CartItemDto;
import com.shop.ecommerce.dto.UpdateCartItemRequest;
import com.shop.ecommerce.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// REST controller handling HTTP requests for cart management.
/// Delegates all logic to CartService and returns DTOs as responses.

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get all cart items for a user
     * GET /api/cart/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable int userId) {
        try {
            List<CartItemDto> cartItems = cartService.getCartItems(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", cartItems);
            response.put("count", cartItems.size());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Add item to cart
     * POST /api/cart/{userId}/items
     */
    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addToCart(@PathVariable int userId, 
                                      @RequestBody AddToCartRequest request) {
        try {
            // Validate request
            if (request.getProductId() <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid product ID");
                return ResponseEntity.badRequest().body(error);
            }

            if (request.getQuantity() <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Quantity must be greater than 0");
                return ResponseEntity.badRequest().body(error);
            }

            CartItemDto cartItem = cartService.addToCart(userId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item added to cart successfully");
            response.put("data", cartItem);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/{userId}/items/{itemId}
     */
    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable int userId,
                                           @PathVariable int itemId,
                                           @RequestBody UpdateCartItemRequest request) {
        try {
            // Validate request
            if (request.getQuantity() <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Quantity must be greater than 0");
                return ResponseEntity.badRequest().body(error);
            }

            CartItemDto cartItem = cartService.updateCartItem(userId, itemId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart item updated successfully");
            response.put("data", cartItem);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/{userId}/items/{itemId}
     */
    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable int userId,
                                           @PathVariable int itemId) {
        try {
            cartService.removeFromCart(userId, itemId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Item removed from cart successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Clear all items from cart
     * DELETE /api/cart/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable int userId) {
        try {
            cartService.clearCart(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cart cleared successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
