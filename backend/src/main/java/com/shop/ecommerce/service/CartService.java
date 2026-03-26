package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.AddToCartRequest;
import com.shop.ecommerce.dto.CartItemDto;
import com.shop.ecommerce.dto.ProductSummaryDto;
import com.shop.ecommerce.dto.UpdateCartItemRequest;
import com.shop.ecommerce.model.CartItem;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Service containing business logic for cart management.
/// Handles adding, updating, removing, and retrieving cart items.

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, 
                      UserRepository userRepository,
                      ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get all cart items for a user
     */
    public List<CartItemDto> getCartItems(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        List<CartItemDto> cartItemDtos = new ArrayList<>();

        for (CartItem item : cartItems) {
            cartItemDtos.add(toDto(item));
        }

        return cartItemDtos;
    }

    /**
     * Add item to cart or update quantity if item already exists
     */
    @Transactional
    public CartItemDto addToCart(int userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        // Check if product is in stock
        if (product.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getQuantity());
        }

        // Check if item already exists in cart
        List<CartItem> existingItems = cartItemRepository.findByUserAndProduct(user, product);
        
        CartItem cartItem;
        if (!existingItems.isEmpty()) {
            // Update existing item quantity
            cartItem = existingItems.get(0);
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            // Update price to current product price
            cartItem.setPriceAtAdd(product.getPrice());
        } else {
            // Create new cart item
            Map<String, String> options = request.getOptions() != null ? request.getOptions() : new HashMap<>();
            cartItem = new CartItem(user, product, request.getQuantity(), product.getPrice(), options);
        }

        CartItem saved = cartItemRepository.save(cartItem);
        return toDto(saved);
    }

    /**
     * Update cart item quantity
     */
    @Transactional
    public CartItemDto updateCartItem(int userId, int itemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + itemId));

        // Verify the cart item belongs to the user
        if (cartItem.getUser().getId() != userId) {
            throw new RuntimeException("Cart item does not belong to user: " + userId);
        }

        // Validate quantity
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        // Check product stock
        Product product = cartItem.getProduct();
        if (product.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        CartItem saved = cartItemRepository.save(cartItem);
        return toDto(saved);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public void removeFromCart(int userId, int itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + itemId));

        // Verify the cart item belongs to the user
        if (cartItem.getUser().getId() != userId) {
            throw new RuntimeException("Cart item does not belong to user: " + userId);
        }

        cartItemRepository.deleteById(itemId);
    }

    /**
     * Clear all items from user's cart
     */
    @Transactional
    public void clearCart(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        cartItemRepository.deleteAll(cartItems);
    }

    /**
     * Convert CartItem entity to CartItemDto
     */
    private CartItemDto toDto(CartItem item) {
        Product product = item.getProduct();

        ProductSummaryDto productDto = new ProductSummaryDto(
                product.getId(),
                product.getProductName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory(),
                product.getSubCategory(),
                product.getBrand(),
                product.getViews(),
                product.getRating()
        );

        return new CartItemDto(
                item.getId(),
                product.getId(),
                product.getProductName(),
                product.getImageUrl(),
                product.getPrice(),
                item.getPriceAtAdd(),
                item.getQuantity(),
                item.getAddedAt(),
                item.getOptions(),
                productDto
        );
    }
}
