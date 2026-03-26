package com.shop.ecommerce.messaging.publisher;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class MLEventPublisher {

  private static final String PRODUCT_CSV_HEADER =
          "id,product_name,price,quantity,views,created_at,updated_at,main_category,sub_category,description,image_url,brand";

  private static final String USER_READY_CSV_HEADER =
          "id";

  private final String exchange;
  private final String mlRKey;
  private final RabbitTemplate rabbitTemplate;

  public MLEventPublisher(RabbitTemplate rabbitTemplate,
                          @Value("${rabbitmq.topicexchange.shop}") String exchange,
                          @Value("${rabbitmq.routingkey.ml}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.mlRKey = routingKey;
  }

  // PRODUCT CSV
  public void publishProductCsv(Product p) {
    String row = toProductCsvRow(p);
    String csv = PRODUCT_CSV_HEADER + "\n" + row + "\n";
    sendCsv("behavior.product_sync", csv); // recommended to not reuse mlRKey blindly
  }

  private static String toProductCsvRow(Product p) {
    return String.join(",",
            String.valueOf(p.getId()),
            csvEscape(p.getProductName()),
            String.valueOf(p.getPrice()),
            String.valueOf(p.getQuantity()),
            String.valueOf(p.getViews()),
            csvEscape(p.getCreatedAt() == null ? "" : p.getCreatedAt().toString()),
            csvEscape(p.getUpdatedAt() == null ? "" : p.getUpdatedAt().toString()),
            csvEscape(p.getCategory()),
            csvEscape(p.getSubCategory()),
            csvEscape(p.getDescription()),
            csvEscape(p.getImageUrl()),
            csvEscape(p.getBrand())
    );
  }

  // USER READY CSV
  public void publishUserReadyCsv(User u) {
    String csv = USER_READY_CSV_HEADER + "\n" + u.getId() + "\n";
    sendCsv("behavior.ready_for_classification", csv);
  }

  private static String toUserReadyCsvRow(User u, LocalDateTime occurredAt) {
    return String.join(",",
            String.valueOf(u.getId()),
            csvEscape(occurredAt == null ? "" : occurredAt.toString())
    );
  }

  // shared CSV send
  private void sendCsv(String routingKey, String csv) {
    MessageProperties props = new MessageProperties();
    props.setContentType("text/csv");
    props.setContentEncoding("utf-8");

    rabbitTemplate.send(exchange, routingKey,
            new Message(csv.getBytes(StandardCharsets.UTF_8), props));
  }

  private static String csvEscape(String s) {
    if (s == null) return "\"\"";
    String escaped = s.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }
}