package com.shop.ecommerce.messaging.consumer;

import com.shop.ecommerce.messaging.dto.ProductEventDto;
import com.shop.ecommerce.service.ElasticService;
import com.shop.ecommerce.service.ProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticEventConsumer {

  private final ElasticService elasticService;
  private final ProductIndexService productIndexService;


  @RabbitListener(queues = "${rabbitmq.queue.elastic}")
  public void handleMessage(ProductEventDto event){
    var p = event.getProduct();
    try { productIndexService.upsert(String.valueOf(p.getId()),p); } catch (Exception e) {
      log.error("ES indexing failed for product {}", p.getId(), e);
      throw new RuntimeException(e);
    }
    log.info("Received from Rabbit: {}", event);

    switch (event.getEventType()) {
      case CREATED -> elasticService.index(event.getProduct());
      case UPDATED -> elasticService.update(event.getProduct());
      case DELETED -> elasticService.delete(event.getProduct());
      default -> throw new IllegalStateException(
              "Unknown event type: " + event.getEventType()
      );
    }



  }


}
