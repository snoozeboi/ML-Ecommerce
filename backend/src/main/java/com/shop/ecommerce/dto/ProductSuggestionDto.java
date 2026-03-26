package com.shop.ecommerce.dto;

/// DTO for one suggestion in the searchbox.
/// Created in ProductService after products are fetched (from DB or Elasticsearch).
/// A list of these is wrapped by SuggestResponseDto and sent to the frontend.

public record ProductSuggestionDto(
        long id,
        String name,
        String category,
        float rating,
        float price,
        float finalPrice,
        Integer discountPercent, // null if none
        String imageUrl
) {}