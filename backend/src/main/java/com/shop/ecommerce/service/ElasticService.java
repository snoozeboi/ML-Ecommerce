package com.shop.ecommerce.service;

import com.shop.ecommerce.messaging.dto.ProductSyncDto;
import org.springframework.stereotype.Service;

@Service
public class ElasticService {

  public void index(ProductSyncDto dto) {
    // write to Elasticsearch
  }

  public void update(ProductSyncDto dto) {
    // update document in Elasticsearch
  }

  public void delete(ProductSyncDto dto) {
    //
  }
}
