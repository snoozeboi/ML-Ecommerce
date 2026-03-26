package com.shop.ecommerce.controller;

import com.shop.ecommerce.service.EventService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping("/product-view/{productId}")
  public void productView(@PathVariable int productId, @RequestParam int userId) {
    eventService.onProductViewed(userId, productId);
  }
}