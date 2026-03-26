package com.shop.ecommerce.messaging.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserBehaviorPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;

  public UserBehaviorPublisher(RabbitTemplate rabbitTemplate,
                               @Value("${rabbitmq.topicexchange.shop}") String exchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
  }

  public void publishReadyForClassification(int userId, LocalDateTime occurredAt) {
    var payload = new UserReadyEvent(userId, occurredAt);
    rabbitTemplate.convertAndSend(exchange, "user.ready_for_classification", payload);
  }

  public void publishReadyForReclassification(int userId, LocalDateTime occurredAt) {
    var payload = new UserReadyEvent(userId, occurredAt);
    rabbitTemplate.convertAndSend(exchange, "user.ready_for_reclassification", payload);
  }

  public record UserReadyEvent(int userId, LocalDateTime occurredAt) {}
}