package com.shop.ecommerce.service;

import com.shop.ecommerce.messaging.publisher.MLEventPublisher;
import com.shop.ecommerce.model.*;
import com.shop.ecommerce.repository.UserViewHistoryRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class EventService {

  private final MLEventPublisher mlPublisher;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final UserViewHistoryRepository userViewHistoryRepository;

  private static final int numToClassify = 5;
  private static final int numToReclassify = 15;
  private static final long hoursToReclassify = 24;

  public EventService(UserRepository userRepository,
                      ProductRepository productRepository,
                      UserViewHistoryRepository userViewHistoryRepository,
                      MLEventPublisher mlPublisher) {
    this.userRepository = userRepository;
    this.productRepository = productRepository;
    this.userViewHistoryRepository = userViewHistoryRepository;
    this.mlPublisher = mlPublisher;
  }

  public void onProductViewed(int userId, int productId) {
    var now = LocalDateTime.now();

    var p = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product doesn't exist : " +productId));


    var u = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User doesn't exist :" + userId));

    p.setViews(p.getViews()+1);
    productRepository.save(p);

    var id = new UserViewHistoryId(userId,productId);
    var vh = userViewHistoryRepository.findById(id)
                    .orElseGet(() -> new UserViewHistory(u, productId));

    vh.setViewCount(vh.getViewCount()+1);
    vh.setLastViewedAt(now);

    userViewHistoryRepository.save(vh);

    u.setEventCounter(u.getEventCounter()+1);
    u.setLastActivity(now);
    userRepository.save(u);


    if (u.getSegment() == UserSegment.UNCLASSIFIED) {
      if(u.getEventCounter() >= numToClassify) {
        mlPublisher.publishUserReadyCsv(u);
        u.setEventCounter(0);
        userRepository.save(u);
      }
    }
    else {
      if (u.getLastClassifiedAt() != null) {
        long hours = Duration.between(u.getLastClassifiedAt(),now).toHours();
        if (u.getEventCounter() >= numToReclassify && hours >= hoursToReclassify) {
          mlPublisher.publishUserReadyCsv(u);
          u.setEventCounter(0);
          userRepository.save(u);
        }
      }
    }
  }
}
