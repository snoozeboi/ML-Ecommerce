package com.shop.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ProductIndexService {
  private final RestClient restClient;
  private final String indexName;

  public ProductIndexService(
          @Value("${spring.elasticsearch.uris}") String esUri,
          @Value("${es.index.products:products}") String indexName
  ) {
    this.restClient = RestClient.builder().baseUrl(esUri).build();
    this.indexName = indexName;
  }

  public void upsert(String id, Object doc) {
    // PUT /{index}/_doc/{id}
    restClient.put()
            .uri("/{index}/_doc/{id}", indexName, id)
            .body(doc)
            .retrieve()
            .toBodilessEntity();
  }
}