package com.shop.ecommerce.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductSyncDto {
  private int id;
  private String productName;
  private String description;
  private String category;
  private String subCategory;
  private String brand;
  private float price;
  private int quantity;
  private String imageUrl;
  private List<String> tags;
  private int views;
  private float rating;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public ProductSyncDto() {}

  public ProductSyncDto(int id, String productName, String description, String category, String subCategory,
                        String brand, float price, int quantity, String imageUrl,
                        List<String> tags, int views, float rating, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.productName = productName;
    this.description = description;
    this.category = category;
    this.subCategory = subCategory;
    this.brand = brand;
    this.price = price;
    this.quantity = quantity;
    this.imageUrl = imageUrl;
    this.tags = tags;
    this.views = views;
    this.rating = rating;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static ProductSyncDto fromProduct(com.shop.ecommerce.model.Product p) {
    return new ProductSyncDto(
            p.getId(),
            p.getProductName(),
            p.getDescription(),
            p.getCategory(),
            p.getSubCategory(),
            p.getBrand(),
            p.getPrice(),
            p.getQuantity(),
            p.getImageUrl(),
            p.getTags(),
            p.getViews(),
            p.getRating(),
            p.getCreatedAt(),
            p.getUpdatedAt()
    );
  }



}
