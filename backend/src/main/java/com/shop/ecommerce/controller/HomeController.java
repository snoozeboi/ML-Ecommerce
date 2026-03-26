package com.shop.ecommerce.controller;


import com.shop.ecommerce.service.RecommendationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.shop.ecommerce.dto.ProductSummaryDto;

@RestController
@RequestMapping("/api/home")
public class HomeController {
  private final RecommendationService recommendationService;

  public HomeController(RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
  }

  @GetMapping("/trending")
  public List<ProductSummaryDto> trending(@RequestParam(defaultValue = "5") int limit) {
    return recommendationService.getTrending(limit);
  }
}
