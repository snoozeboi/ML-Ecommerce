package com.shop.ecommerce.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductEventDto {

  private ProductEventType eventType; //CREATE/UPDATE/DELETE
  private ProductSyncDto product; //dto itself




}
