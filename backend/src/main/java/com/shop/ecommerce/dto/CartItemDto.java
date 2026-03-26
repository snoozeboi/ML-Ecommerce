package com.shop.ecommerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/// DTO for returning cart items to the frontend.
/// Contains all necessary information about a cart item.

@Getter
@Setter
public class CartItemDto {
    private int id;
    private int productId;
    private String productName;
    private String productImageUrl;
    private float price;
    private float priceAtAdd;
    private int quantity;
    private LocalDateTime addedAt;
    private Map<String, String> options;
    private ProductSummaryDto product; // Full product details

    public CartItemDto() {
    }

    public CartItemDto(int id, int productId, String productName, String productImageUrl, 
                      float price, float priceAtAdd, int quantity, LocalDateTime addedAt, 
                      Map<String, String> options, ProductSummaryDto product) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.price = price;
        this.priceAtAdd = priceAtAdd;
        this.quantity = quantity;
        this.addedAt = addedAt;
        this.options = options;
        this.product = product;
    }
}
