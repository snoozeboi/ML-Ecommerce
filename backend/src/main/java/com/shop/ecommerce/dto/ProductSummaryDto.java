package com.shop.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductSummaryDto {
  private int id;
  private String productName;
  private float price;
  private String imageUrl;
  private String category;
  private String subCategory;
  private String brand;
  private int views;
  private float rating;
}