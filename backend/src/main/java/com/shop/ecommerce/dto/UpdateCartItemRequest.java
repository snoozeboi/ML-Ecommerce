package com.shop.ecommerce.dto;

/// Request DTO for updating cart item quantity.
/// Built by Spring from incoming JSON (@RequestBody).

public class UpdateCartItemRequest {
    private int quantity;

    public UpdateCartItemRequest() {
    }

    public UpdateCartItemRequest(int quantity) {
        this.quantity = quantity;
    }

    // Explicit getter to ensure compatibility (Lombok should generate this, but adding for safety)
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
