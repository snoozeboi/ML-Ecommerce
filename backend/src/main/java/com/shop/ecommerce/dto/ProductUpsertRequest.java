package com.shop.ecommerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/// Request DTO used when creating or updating a product.
/// Built by Spring from incoming JSON (@RequestBody).
/// Used only as input to ProductService.

@Getter
@Setter
public class ProductUpsertRequest {
  private String productName;
  private String description;
  private String category;  // Optional - will be set by ML categorization if not provided
  private String subCategory;  // Optional - will be set by ML categorization if not provided
  private String brand;
  private float price;
  private int quantity;
  private String imageUrl;
  private List<String> tags;  // Optional - will be set by ML categorization if not provided
  private float rating;
  private int views;
}