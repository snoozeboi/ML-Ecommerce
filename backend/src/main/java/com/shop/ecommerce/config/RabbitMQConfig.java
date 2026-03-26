package com.shop.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This class configures rabbitMQ mapping. defines an exchange, we use only 1 exchange, theres no need for more. 1 exchange to multiple queues. the queues are mlqueue and elasticqueue.
// as well as the routing keys. whenever a message comes to the broker, exchange navigates the message using the key to corresponding queue by the binding rule.

@Configuration
public class RabbitMQConfig {

  @Value("${rabbitmq.topicexchange.shop}")
  private String exchange;

  @Value("${rabbitmq.queue.ml}")
  private String mlQueue;

  @Value("${rabbitmq.routingkey.ml}")
  private String mlRKey;

  @Value("${rabbitmq.queue.elastic}")
  private String elasticQueue;

  @Value("${rabbitmq.routingkey.elastic}")
  private String elasticRKey;

  // bean for rabbitmq exchange, elastic+ML
  @Bean
  public TopicExchange exchange(){
    return new TopicExchange(exchange);
  }

  // bean for rabbitmq ML queue
  @Bean
  public Queue mlQueue(){
    return new Queue(mlQueue);
  }

  // bean for rabbitmq elastic queue
  @Bean
  public Queue elasticQueue(){
    return new Queue(elasticQueue);
  }

  // binding rule for ml queue using ml routing key
  @Bean
  public Binding mlBinding(){
    return BindingBuilder.bind(mlQueue())
            .to(exchange())
            .with(mlRKey);
  }

  // binding rule for elastic queue using elastic routing key
  @Bean
  public Binding elasticBinding(){
    return BindingBuilder.bind(elasticQueue())
            .to(exchange())
            .with(elasticRKey);
  }

  @Bean
  public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  // Connection factory, Rabbit template, Rabbit admin will be configured by spring.

}
