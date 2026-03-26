package com.shop.ecommerce.controller;

import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.UserViewHistoryRepository;
import com.shop.ecommerce.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * REST Controller for Recommendation System
 * 
 * Provides endpoints for:
 * - Getting recommendations for users (guest and registered)
 * - Getting similar products for product detail pages
 * - Getting trending products
 * - Getting category-based recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserViewHistoryRepository userViewHistoryRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private com.shop.ecommerce.service.EventService eventService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ML_SERVICE_URL = "http://localhost:5000";

    /** Skip re-sending full payload to ML for this long (ms). Speeds up recommendation requests. */
    private static final long ML_DATA_LOAD_TTL_MS = 45_000L;
    private volatile long lastDataLoadTime = 0;
    private final Object dataLoadLock = new Object();

    /**
     * Call after a purchase so the next recommendation request refreshes ML data.
     */
    public void invalidateMLDataCache() {
        lastDataLoadTime = 0;
    }
    
    /**
     * Force reload of all ML data (bypasses TTL cache).
     * Used immediately after purchases to ensure ML service has latest data.
     */
    public void forceReloadMLData() {
        synchronized (dataLoadLock) {
            lastDataLoadTime = 0; // Reset cache
            try {
                Map<String, Object> dataPayload = recommendationService.buildMLPayload();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(dataPayload, headers);
                restTemplate.postForEntity(ML_SERVICE_URL + "/data/load", request, Map.class);
                lastDataLoadTime = System.currentTimeMillis();
                System.out.println("[RecommendationController] Force reloaded ML data after purchase");
            } catch (Exception e) {
                System.err.println("Error force reloading ML data: " + e.getMessage());
            }
        }
    }
    
    /**
     * Record a single purchase interaction to ML service.
     * Used to immediately update ML service with new purchases.
     */
    public void recordPurchaseInteraction(int userId, int productId) {
        try {
            Map<String, Object> interactionData = new HashMap<>();
            interactionData.put("user_id", userId);
            interactionData.put("product_id", productId);
            interactionData.put("interaction_type", "purchase");
            interactionData.put("weight", 3); // Higher weight for purchases
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(interactionData, headers);
            restTemplate.postForEntity(ML_SERVICE_URL + "/interactions/record", request, Map.class);
            System.out.println("[RecommendationController] Recorded purchase interaction: user=" + userId + ", product=" + productId);
        } catch (Exception e) {
            System.err.println("Error recording purchase interaction: " + e.getMessage());
        }
    }
    
    /** Image URLs by product type (Pexels) – used only as fallbacks when a product has no imageUrl in DB. */
    private static final String IMG_BACKPACK = "https://images.pexels.com/photos/4475708/pexels-photo-4475708.jpeg";
    private static final String IMG_TOTE = "https://images.pexels.com/photos/5926240/pexels-photo-5926240.jpeg";
    private static final String IMG_MESSENGER = "https://images.pexels.com/photos/3731256/pexels-photo-3731256.jpeg";
    private static final String IMG_BELT_BAG = "https://images.pexels.com/photos/2905238/pexels-photo-2905238.jpeg";
    private static final String IMG_CROSSBODY = "https://images.pexels.com/photos/2422476/pexels-photo-2422476.jpeg";

    private String getImageUrlForProduct(Product p) {
        if (p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
            return p.getImageUrl();
        }
        String name = (p.getProductName() != null ? p.getProductName() : "").toLowerCase();
        if (name.contains("backpack")) return IMG_BACKPACK;
        if (name.contains("tote")) return IMG_TOTE;
        if (name.contains("utility")) return IMG_TOTE;
        if (name.contains("messenger")) return IMG_MESSENGER;
        if (name.contains("belt")) return IMG_BELT_BAG;
        if (name.contains("crossbody")) return IMG_CROSSBODY;
        return IMG_BACKPACK;
    }
    
    /**
     * Load data to Python ML service. Uses RecommendationService so payload is built inside
     * a transaction and lazy collections (e.g. User.purchaseHistory) are loaded.
     * Skips loading if data was sent recently (see ML_DATA_LOAD_TTL_MS) to speed up requests.
     */
    private void loadDataToMLService() {
        long now = System.currentTimeMillis();
        if (now - lastDataLoadTime < ML_DATA_LOAD_TTL_MS) {
            return;
        }
        synchronized (dataLoadLock) {
            if (System.currentTimeMillis() - lastDataLoadTime < ML_DATA_LOAD_TTL_MS) {
                return;
            }
            try {
                Map<String, Object> dataPayload = recommendationService.buildMLPayload();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(dataPayload, headers);
                restTemplate.postForEntity(ML_SERVICE_URL + "/data/load", request, Map.class);
                lastDataLoadTime = System.currentTimeMillis();
            } catch (Exception e) {
                System.err.println("Error loading data to ML service: " + e.getMessage());
            }
        }
    }

    /** Run loadDataToMLService in background so we don't block the request. */
    private void loadDataToMLServiceAsync() {
        new Thread(() -> {
            try { loadDataToMLService(); } catch (Exception e) { /* ignore */ }
        }).start();
    }

    /** True if ML data was loaded recently (within TTL). When false, we return DB fallback and load in background. */
    private boolean isMLDataFresh() {
        return lastDataLoadTime > 0 && (System.currentTimeMillis() - lastDataLoadTime) < ML_DATA_LOAD_TTL_MS;
    }

    /** Fast fallback: popular products from DB (sorted by views). */
    private List<Map<String, Object>> fallbackPopularProducts(int limit, int offset) {
        List<Product> all = productRepository.findAll();
        List<Product> sorted = all.stream()
                .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
                .limit(offset + limit)
                .collect(Collectors.toList());
        List<Map<String, Object>> maps = productsToMapsWithUniqueImages(sorted, offset + limit);
        if (maps.size() <= offset) return new ArrayList<>();
        int to = Math.min(offset + limit, maps.size());
        return new ArrayList<>(maps.subList(offset, to));
    }
    
    /**
     * Fallback when ML service is unavailable: recommend products from same categories
     * as the user's purchase history (and cart), excluding items they already have.
     */
    private List<Map<String, Object>> fallbackPersonalizedRecommendations(User user, int limit) {
        Set<Integer> excludeIds = new HashSet<>();
        if (user.getPurchaseHistory() != null) {
            excludeIds.addAll(user.getPurchaseHistory());
        }
        for (com.shop.ecommerce.model.CartItem item : cartItemRepository.findByUser(user)) {
            excludeIds.add(item.getProduct().getId());
        }
        Set<String> preferredCategories = new HashSet<>();
        for (Integer pid : excludeIds) {
            productRepository.findById(pid).ifPresent(p -> {
                if (p.getCategory() != null && !p.getCategory().isBlank()) {
                    preferredCategories.add(p.getCategory());
                }
            });
        }
        List<Product> all = productRepository.findAll();
        List<Product> candidates = all.stream()
                .filter(p -> !excludeIds.contains(p.getId()))
                .filter(p -> p.getCategory() != null && preferredCategories.contains(p.getCategory()))
                .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
                .limit(limit)
                .collect(Collectors.toList());
        if (candidates.size() < limit) {
            Set<Integer> addedIds = candidates.stream().map(Product::getId).collect(Collectors.toSet());
            for (Product p : all) {
                if (addedIds.size() >= limit) break;
                if (!excludeIds.contains(p.getId()) && !addedIds.contains(p.getId())) {
                    candidates.add(p);
                    addedIds.add(p.getId());
                }
            }
        }
        return productsToMapsWithUniqueImages(candidates, limit);
    }

    /** Convert products to list of maps with imageUrl matching product type */
    private List<Map<String, Object>> productsToMapsWithUniqueImages(List<Product> products, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        int count = 0;
        for (Product p : products) {
            if (count >= limit) break;
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("productName", p.getProductName());
            map.put("description", p.getDescription());
            map.put("category", p.getCategory());
            map.put("subCategory", p.getSubCategory());
            map.put("brand", p.getBrand());
            map.put("price", p.getPrice());
            map.put("quantity", p.getQuantity());
            map.put("views", p.getViews());
            map.put("rating", p.getRating());
            map.put("imageUrl", getImageUrlForProduct(p));
            map.put("tags", p.getTags());
            result.add(map);
            count++;
        }
        return result;
    }
    
    /**
     * Get recommendations for a user
     * 
     * @param userId The user ID
     * @param limit Maximum number of recommendations (default: 10)
     * @return List of recommended products
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecommendations(
            @PathVariable int userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        User user = userOpt.get();
        
        // Load data to ML service first
        loadDataToMLService();
        
        // Call Python ML service
        String url = ML_SERVICE_URL + "/recommendations/personalized/" + userId + "?limit=" + limit;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    
    /**
     * Get similar products for a product detail page
     * 
     * @param productId The product ID
     * @param limit Maximum number of similar products (default: 5)
     * @return List of similar products
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<?> getSimilarProducts(
            @PathVariable int productId,
            @RequestParam(defaultValue = "5") int limit) {
        
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // Load data to ML service first
        loadDataToMLService();
        
        // Call Python ML service
        String url = ML_SERVICE_URL + "/recommendations/similar/" + productId + "?limit=" + limit;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    
    /**
     * Get trending products (popular purchases). Supports offset for "load next" without recomputing ranking.
     *
     * @param limit  Number of products to return (default: 10)
     * @param offset Skip this many in the pre-ranked list (default: 0)
     * @return List of trending products
     */
    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        if (!isMLDataFresh()) {
            loadDataToMLServiceAsync();
            return ResponseEntity.ok(fallbackPopularProducts(limit, offset));
        }
        loadDataToMLService();
        String url = ML_SERVICE_URL + "/recommendations/trending?limit=" + limit + "&offset=" + offset;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallbackPopularProducts(limit, offset));
        }
    }
    
    /**
     * Get recommendations by category
     * 
     * @param category The category name
     * @param limit Maximum number of recommendations (default: 10)
     * @return List of products in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getRecommendationsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        
        // Load data to ML service first
        loadDataToMLService();
        
        // Call Python ML service
        String url = ML_SERVICE_URL + "/recommendations/category/" + category + "?limit=" + limit;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    
    /**
     * Record a product view (for analytics and ML). Persists to DB when userId is provided so
     * the interaction is used on next /data/load; also notifies ML service for immediate updates.
     *
     * @param productId The product ID that was viewed
     * @param userId    Optional. When provided, the view is persisted to PostgreSQL (user_view_history, products.views) for use later.
     * @return Success response
     */
    @PostMapping("/view/{productId}")
    public ResponseEntity<Map<String, String>> recordProductView(
            @PathVariable int productId,
            @RequestParam(required = false) Integer userId) {
        int effectiveUserId = (userId != null && userId > 0) ? userId : 1;

        // Persist to PostgreSQL so this view is included in future /data/load and used by ML later
        if (userId != null && userId > 0) {
            try {
                eventService.onProductViewed(userId, productId);
            } catch (Exception e) {
                // Log but continue to notify ML
                System.err.println("EventService.onProductViewed failed: " + e.getMessage());
            }
        }

        // Notify ML service for immediate in-memory update
        Map<String, Object> interactionData = new HashMap<>();
        interactionData.put("user_id", effectiveUserId);
        interactionData.put("product_id", productId);
        interactionData.put("interaction_type", "view");
        interactionData.put("weight", 1);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(interactionData, headers);
        try {
            restTemplate.postForEntity(ML_SERVICE_URL + "/interactions/record", request, Map.class);
        } catch (Exception e) {
            System.err.println("ML interactions/record failed: " + e.getMessage());
        }
        Map<String, String> successMap = new HashMap<>();
        successMap.put("message", "Product view recorded successfully");
        return ResponseEntity.ok(successMap);
    }
    
    /**
     * Record a search term
     * 
     * @param userId The user ID
     * @param searchData The search data containing the search term
     * @return Success response
     */
    @PostMapping("/search/{userId}")
    public ResponseEntity<Map<String, String>> recordSearch(
            @PathVariable int userId,
            @RequestBody Map<String, String> searchData) {
        
        String searchTerm = searchData.get("searchTerm");
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Search term is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMap);
        }
        
        // Call Python ML service to record interaction
        Map<String, Object> interactionData = new HashMap<>();
        interactionData.put("user_id", userId);
        interactionData.put("product_id", 1); // Default product for search
        interactionData.put("interaction_type", "search");
        interactionData.put("weight", 1);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(interactionData, headers);
        
        try {
            restTemplate.postForEntity(ML_SERVICE_URL + "/interactions/record", request, Map.class);
            Map<String, String> successMap = new HashMap<>();
            successMap.put("message", "Search recorded successfully");
            return ResponseEntity.ok(successMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to record search");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }
    
    /**
     * Record a purchase
     * 
     * @param userId The user ID
     * @param purchaseData The purchase data containing the product ID
     * @return Success response
     */
    @PostMapping("/purchase/{userId}")
    public ResponseEntity<Map<String, String>> recordPurchase(
            @PathVariable int userId,
            @RequestBody Map<String, Integer> purchaseData) {
        
        Integer productId = purchaseData.get("productId");
        if (productId == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Product ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMap);
        }
        
        // Call Python ML service to record interaction
        Map<String, Object> interactionData = new HashMap<>();
        interactionData.put("user_id", userId);
        interactionData.put("product_id", productId);
        interactionData.put("interaction_type", "purchase");
        interactionData.put("weight", 3); // Higher weight for purchases
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(interactionData, headers);
        
        try {
            restTemplate.postForEntity(ML_SERVICE_URL + "/interactions/record", request, Map.class);
            Map<String, String> successMap = new HashMap<>();
            successMap.put("message", "Purchase recorded successfully");
            return ResponseEntity.ok(successMap);
        } catch (Exception e) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to record purchase");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }
    
    /**
     * Get guest recommendations (popular products)
     *
     * @param limit  Maximum number of products to return (default: 10)
     * @param offset Skip this many in the ranked list (default: 0)
     * @return List of popular products
     */
    @GetMapping("/guest")
    public ResponseEntity<?> getGuestRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        if (!isMLDataFresh()) {
            loadDataToMLServiceAsync();
            return ResponseEntity.ok(fallbackPopularProducts(limit, offset));
        }
        loadDataToMLService();
        int requestSize = offset + limit;
        String url = ML_SERVICE_URL + "/recommendations/guest?limit=" + requestSize;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<?> body = response.getBody();
            if (body == null || body.size() <= offset) {
                return ResponseEntity.ok(offset == 0 ? (body != null ? body : new ArrayList<>()) : new ArrayList<>());
            }
            int to = Math.min(offset + limit, body.size());
            return ResponseEntity.ok(body.subList(offset, to));
        } catch (Exception e) {
            // Fallback: return popular products with unique image per product when ML is unavailable
            List<Product> all = productRepository.findAll();
            List<Product> sorted = all.stream()
                    .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
                    .limit(requestSize)
                    .collect(Collectors.toList());
            List<Map<String, Object>> maps = productsToMapsWithUniqueImages(sorted, requestSize);
            if (maps.size() <= offset) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            int to = Math.min(offset + limit, maps.size());
            return ResponseEntity.ok(maps.subList(offset, to));
        }
    }
    
    /**
     * Get personalized recommendations for registered users
     *
     * @param userId The user ID
     * @param limit  Maximum number of products to return (default: 10)
     * @param offset Skip this many in the ranked list (default: 0)
     * @return List of personalized products
     */
    @GetMapping("/personalized/{userId}")
    public ResponseEntity<?> getPersonalizedRecommendations(
            @PathVariable int userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!isMLDataFresh()) {
            loadDataToMLServiceAsync();
            User user = userOpt.get();
            List<Map<String, Object>> fallback = fallbackPersonalizedRecommendations(user, offset + limit);
            if (fallback.size() <= offset) return ResponseEntity.ok(new ArrayList<>());
            int to = Math.min(offset + limit, fallback.size());
            return ResponseEntity.ok(fallback.subList(offset, to));
        }
        loadDataToMLService();

        int requestSize = offset + limit;
        String url = ML_SERVICE_URL + "/recommendations/personalized/" + userId + "?limit=" + requestSize;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<?> body = response.getBody();
            if (body == null || body.size() <= offset) {
                return ResponseEntity.ok(offset == 0 ? (body != null ? body : new ArrayList<>()) : new ArrayList<>());
            }
            int to = Math.min(offset + limit, body.size());
            return ResponseEntity.ok(body.subList(offset, to));
        } catch (Exception e) {
            // ML unavailable: use purchase-history-based fallback so recommendations still make sense
            System.err.println("ML service unavailable, using purchase-history fallback: " + e.getMessage());
            User user = userOpt.get();
            List<Map<String, Object>> fallback = fallbackPersonalizedRecommendations(user, requestSize);
            if (fallback.size() <= offset) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            int to = Math.min(offset + limit, fallback.size());
            return ResponseEntity.ok(fallback.subList(offset, to));
        }
    }
    
    /**
     * Get all available products with unique image per product (Picsum by id)
     * 
     * @return List of products with imageUrl set so each product shows a different image
     */
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("productName", p.getProductName());
            map.put("description", p.getDescription());
            map.put("category", p.getCategory());
            map.put("subCategory", p.getSubCategory());
            map.put("brand", p.getBrand());
            map.put("price", p.getPrice());
            map.put("quantity", p.getQuantity());
            map.put("views", p.getViews());
            map.put("rating", p.getRating());
            map.put("imageUrl", getImageUrlForProduct(p));
            map.put("tags", p.getTags());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
} 