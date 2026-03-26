package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.ProductSummaryDto;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSearchHistory;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.repository.UserViewHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

  private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
  private static final String IMG_FALLBACK = "https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg";

  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final CartItemRepository cartItemRepository;
  private final UserViewHistoryRepository userViewHistoryRepository;

  public RecommendationService(
      ProductRepository productRepository,
      UserRepository userRepository,
      CartItemRepository cartItemRepository,
      UserViewHistoryRepository userViewHistoryRepository) {
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.cartItemRepository = cartItemRepository;
    this.userViewHistoryRepository = userViewHistoryRepository;
  }

  /**
   * Build the payload for the ML service. Runs in a transaction so that lazy collections
   * (e.g. User.purchaseHistory) are loaded before the session closes.
   */
  @Transactional(readOnly = true)
  public Map<String, Object> buildMLPayload() {
    List<User> users = userRepository.findAllWithSearchHistory();
    List<Product> products = productRepository.findAll();
    List<com.shop.ecommerce.model.CartItem> cartItems = cartItemRepository.findAll();

    List<Map<String, Object>> usersData = new ArrayList<>();
    for (User user : users) {
      Map<String, Object> userData = new HashMap<>();
      userData.put("id", user.getId());
      userData.put("userName", user.getUserName());
      userData.put("email", user.getEmail());
      userData.put("isGuest", user.isGuest());
      List<Map<String, Object>> searchHistoryData = new ArrayList<>();
      if (user.getSearchHistory() != null) {
        for (UserSearchHistory sh : user.getSearchHistory()) {
          Map<String, Object> m = new HashMap<>();
          m.put("search_term", sh.getId() != null ? sh.getId().getSearchTerm() : null);
          m.put("search_count", sh.getSearchCount());
          m.put("last_searched_at", sh.getLastSearchedAt() != null ? sh.getLastSearchedAt().toString() : null);
          searchHistoryData.add(m);
        }
      }
      userData.put("searchHistory", searchHistoryData);
      userData.put("purchaseHistory", user.getPurchaseHistory() != null ? new ArrayList<>(user.getPurchaseHistory()) : new ArrayList<>());
      if (user.getMlCategory() != null) {
        userData.put("ml_category", user.getMlCategory());
      }
      usersData.add(userData);
    }

    List<Map<String, Object>> productsData = new ArrayList<>();
    for (Product product : products) {
      Map<String, Object> productData = new HashMap<>();
      productData.put("id", product.getId());
      productData.put("productName", product.getProductName());
      productData.put("description", product.getDescription());
      productData.put("category", product.getCategory());
      if (product.getSubCategory() != null && !product.getSubCategory().isBlank()) {
        productData.put("sub_category", product.getSubCategory());
      }
      productData.put("price", product.getPrice());
      productData.put("quantity", product.getQuantity());
      productData.put("views", product.getViews());
      if (product.getMlCategory() != null) {
        productData.put("ml_category", product.getMlCategory());
      }
      productData.put("rating", product.getRating());
      productData.put("imageUrl", imageUrlFor(product));
      productData.put("tags", product.getTags());
      productsData.add(productData);
    }

    List<Map<String, Object>> interactionsData = new ArrayList<>();
    for (com.shop.ecommerce.model.CartItem cartItem : cartItems) {
      Map<String, Object> interactionData = new HashMap<>();
      interactionData.put("user_id", cartItem.getUser().getId());
      interactionData.put("product_id", cartItem.getProduct().getId());
      interactionData.put("weight", 2);
      interactionData.put("timestamp", cartItem.getAddedAt().toString());
      interactionsData.add(interactionData);
    }
    for (User user : users) {
      List<Integer> ph = user.getPurchaseHistory();
      if (ph != null && !ph.isEmpty()) {
        for (Integer productId : ph) {
          Map<String, Object> interactionData = new HashMap<>();
          interactionData.put("user_id", user.getId());
          interactionData.put("product_id", productId);
          interactionData.put("weight", 3);
          interactionData.put("interaction_type", "purchase");
          interactionsData.add(interactionData);
        }
        log.info("ML payload: user_id={} has {} purchase(s)", user.getId(), ph.size());
      }
    }
    for (com.shop.ecommerce.model.UserViewHistory vh : userViewHistoryRepository.findAll()) {
      Map<String, Object> interactionData = new HashMap<>();
      interactionData.put("user_id", vh.getId().getUserId());
      interactionData.put("product_id", vh.getId().getProductId());
      interactionData.put("weight", 1);
      interactionData.put("interaction_type", "view");
      interactionsData.add(interactionData);
    }

    Map<String, Object> dataPayload = new HashMap<>();
    dataPayload.put("products", productsData);
    dataPayload.put("users", usersData);
    dataPayload.put("interactions", interactionsData);
    return dataPayload;
  }

  private static String imageUrlFor(Product p) {
    if (p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
      return p.getImageUrl();
    }
    return IMG_FALLBACK;
  }

  public List<ProductSummaryDto> getTrending(int limit) {
    List<Product> products = productRepository.findAllByOrderByViewsDesc(PageRequest.of(0, limit));

    return products.stream()
            .map(p -> new ProductSummaryDto(
                    p.getId(),
                    p.getProductName(),
                    p.getPrice(),
                    p.getImageUrl(),
                    p.getCategory(),
                    p.getSubCategory(),
                    p.getBrand(),
                    p.getViews(),
                    p.getRating()
            ))
            .toList();
  }
}