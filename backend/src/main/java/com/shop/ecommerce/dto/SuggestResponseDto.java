package com.shop.ecommerce.dto;

import java.util.List;

/// Wrapper DTO for searchbox/autocomplete responses.
/// Built in the controller or service from a list of ProductSuggestionDto.
/// Sent directly as the HTTP response to the frontend.

public record SuggestResponseDto(
        String query,
        List<ProductSuggestionDto> suggestions
) {}