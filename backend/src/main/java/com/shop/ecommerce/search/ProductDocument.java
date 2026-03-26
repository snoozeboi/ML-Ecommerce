package com.shop.ecommerce.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Document(indexName = "products")
public class ProductDocument {

  @Id
  private Long id;

  private String productName;

  @Field(type = FieldType.Text)
  private String description;

  private String category;
  private String subCategory;

  private String brand;

  private Float price;
  private Long quantity;

  private String imageUrl;

  // ES source shows tags as an array. In mapping its text+keyword, so List<String>
  private List<String> tags;

  private Long views;
  private Float rating;

  // ES "_source" shows createdAt/updatedAt as arrays like [2022,7,9,0,0].
  // To avoid mapping/parsing problems storing them as object for now.
  private Object createdAt;
  private Object updatedAt;
}