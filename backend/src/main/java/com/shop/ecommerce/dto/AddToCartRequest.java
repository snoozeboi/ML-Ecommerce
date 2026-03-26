package com.shop.ecommerce.dto;

import java.util.Map;

/// Request DTO for adding an item to cart.
/// Built by Spring from incoming JSON (@RequestBody).

public class AddToCartRequest {
    private int productId;
    private int quantity;
    private Map<String, String> options; // Optional product options (e.g., size, color)

    public AddToCartRequest() {
    }

    public AddToCartRequest(int productId, int quantity, Map<String, String> options) {
        this.productId = productId;
        this.quantity = quantity;
        this.options = options;
    }

    // Explicit getters to ensure compatibility (Lombok should generate these, but adding for safety)
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
}
