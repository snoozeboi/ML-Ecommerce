//package com.shop.ecommerce.service;
//
//import com.shop.ecommerce.model.User;
//import com.shop.ecommerce.model.CartItem;
//import com.shop.ecommerce.model.Product;
//
//import java.util.*;
//import java.util.stream.Collectors;
//import java.time.LocalDateTime;
//
///**
// * Recommendation Engine for E-commerce Platform
// *
// * This engine provides three types of recommendations:
// * 1. Guest recommendations based on global popularity
// * 2. Personalized recommendations for registered users
// * 3. Similar product recommendations for product detail pages
// */
//public class RecommendationEngine {
//
//    private List<User> users;
//    private List<CartItem> cartItems;
//    private List<Product> products;
//
//    // Cache for performance optimization
//    private Map<Integer, Product> productCache;
//    private Map<Integer, User> userCache;
//
//    public RecommendationEngine(List<User> users, List<CartItem> cartItems, List<Product> products) {
//        this.users = users;
//        this.cartItems = cartItems;
//        this.products = products;
//        initializeCaches();
//    }
//
//    /**
//     * Initialize caches for better performance
//     */
//    private void initializeCaches() {
//        productCache = products.stream()
//                .collect(Collectors.toMap(Product::getId, product -> product));
//        userCache = users.stream()
//                .collect(Collectors.toMap(User::getId, user -> user));
//    }
//
//    /**
//     * Get recommendations for a user (guest or registered)
//     *
//     * @param user The user requesting recommendations
//     * @param limit Maximum number of recommendations to return
//     * @return List of recommended products
//     */
//    public List<Product> getRecommendations(User user, int limit) {
//        if (user.isGuest()) {
//            return getGuestRecommendations(limit);
//        } else {
//            return getPersonalizedRecommendations(user, limit);
//        }
//    }
//
//    /**
//     * Get popular product recommendations for guest users
//     * Based on global behavior: searches, purchases, and cart additions
//     *
//     * @param limit Maximum number of recommendations
//     * @return List of popular products
//     */
//    public List<Product> getGuestRecommendations(int limit) {
//        Map<Integer, Integer> productScores = new HashMap<>();
//
//        // Count searches across all users
//        for (User user : users) {
//            for (String searchTerm : user.getSearchHistory()) {
//                // For simplicity, assume search terms can be product IDs
//                try {
//                    int productId = Integer.parseInt(searchTerm);
//                    productScores.merge(productId, 1, Integer::sum);
//                } catch (NumberFormatException e) {
//                    // Skip non-numeric search terms
//                }
//            }
//        }
//
//        // Count purchases across all users
//        for (User user : users) {
//            for (Integer productId : user.getPurchaseHistory()) {
//                productScores.merge(productId, 3, Integer::sum); // Purchases weighted higher
//            }
//        }
//
//        // Count cart additions across all users
//        for (CartItem cartItem : cartItems) {
//            productScores.merge(cartItem.getProduct().getId(), 2, Integer::sum); // Cart additions weighted medium
//        }
//
//        // Sort by score and return top products
//        return productScores.entrySet().stream()
//                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
//                .limit(limit)
//                .map(entry -> productCache.get(entry.getKey()))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get personalized recommendations for registered users
//     * Based only on the user's own search and purchase history
//     *
//     * @param user The registered user
//     * @param limit Maximum number of recommendations
//     * @return List of personalized products
//     */
//    public List<Product> getPersonalizedRecommendations(User user, int limit) {
//        Map<Integer, Integer> productScores = new HashMap<>();
//
//        // Analyze user's search history
//        for (String searchTerm : user.getSearchHistory()) {
//            try {
//                int productId = Integer.parseInt(searchTerm);
//                productScores.merge(productId, 1, Integer::sum);
//            } catch (NumberFormatException e) {
//                // Skip non-numeric search terms
//            }
//        }
//
//        // Analyze user's purchase history
//        for (Integer productId : user.getPurchaseHistory()) {
//            productScores.merge(productId, 3, Integer::sum); // Purchases weighted higher
//        }
//
//        // Get products from user's cart history
//        for (CartItem cartItem : user.getCartItems()) {
//            productScores.merge(cartItem.getProduct().getId(), 2, Integer::sum);
//        }
//
//        // If user has no history, fall back to category-based recommendations
//        if (productScores.isEmpty()) {
//            return getCategoryBasedRecommendations(user, limit);
//        }
//
//        // Sort by score and return top products
//        return productScores.entrySet().stream()
//                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
//                .limit(limit)
//                .map(entry -> productCache.get(entry.getKey()))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get category-based recommendations when user has no history
//     *
//     * @param user The user
//     * @param limit Maximum number of recommendations
//     * @return List of category-based products
//     */
//    private List<Product> getCategoryBasedRecommendations(User user, int limit) {
//        // For users with no history, return most viewed products
//        return products.stream()
//                .sorted(Comparator.comparing(Product::getViews).reversed())
//                .limit(limit)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get similar products for a product detail page
//     * Based on category, tags, price range, and view count
//     *
//     * @param product The product to find similar items for
//     * @param limit Maximum number of similar products
//     * @return List of similar products
//     */
//    public List<Product> getSimilarProducts(Product product, int limit) {
//        if (product == null) {
//            return new ArrayList<>();
//        }
//
//        Map<Product, Double> similarityScores = new HashMap<>();
//
//        for (Product otherProduct : products) {
//            if (otherProduct.getId() == product.getId()) {
//                continue; // Skip the same product
//            }
//
//            double score = calculateSimilarityScore(product, otherProduct);
//            similarityScores.put(otherProduct, score);
//        }
//
//        // Sort by similarity score and return top products
//        return similarityScores.entrySet().stream()
//                .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
//                .limit(limit)
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Calculate similarity score between two products
//     *
//     * @param product1 First product
//     * @param product2 Second product
//     * @return Similarity score (higher = more similar)
//     */
//    private double calculateSimilarityScore(Product product1, Product product2) {
//        double score = 0.0;
//
//        // Category match (highest weight)
//        if (product1.getCategory().equals(product2.getCategory())) {
//            score += 5.0;
//        }
//
//        // Tag overlap
//        Set<String> tags1 = new HashSet<>(product1.getTags());
//        Set<String> tags2 = new HashSet<>(product2.getTags());
//        Set<String> intersection = new HashSet<>(tags1);
//        intersection.retainAll(tags2);
//        score += intersection.size() * 2.0;
//
//        // Price similarity (within 20% range)
//        double priceDiff = Math.abs(product1.getPrice() - product2.getPrice()) / product1.getPrice();
//        if (priceDiff <= 0.2) {
//            score += 3.0 * (1.0 - priceDiff);
//        }
//
//        // View count similarity (both popular or both niche)
//        double viewDiff = Math.abs(product1.getViews() - product2.getViews()) /
//                         Math.max(product1.getViews(), product2.getViews());
//        if (viewDiff <= 0.3) {
//            score += 1.0 * (1.0 - viewDiff);
//        }
//
//        return score;
//    }
//
//    /**
//     * Get recommendations by category
//     *
//     * @param category The category to get recommendations for
//     * @param limit Maximum number of recommendations
//     * @return List of products in the category
//     */
//    public List<Product> getRecommendationsByCategory(String category, int limit) {
//        return products.stream()
//                .filter(product -> product.getCategory().equalsIgnoreCase(category))
//                .sorted(Comparator.comparing(Product::getViews).reversed())
//                .limit(limit)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get trending products based on recent activity
//     *
//     * @param limit Maximum number of trending products
//     * @return List of trending products
//     */
//    public List<Product> getTrendingProducts(int limit) {
//        Map<Integer, Integer> recentActivity = new HashMap<>();
//
//        // Count recent cart additions (last 7 days)
//        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
//        for (CartItem cartItem : cartItems) {
//            if (cartItem.getAddedAt().isAfter(weekAgo)) {
//                recentActivity.merge(cartItem.getProduct().getId(), 1, Integer::sum);
//            }
//        }
//
//        // Count recent purchases
//        for (User user : users) {
//            // This is a simplified version - in a real system, you'd track purchase timestamps
//            for (Integer productId : user.getPurchaseHistory()) {
//                recentActivity.merge(productId, 2, Integer::sum);
//            }
//        }
//
//        return recentActivity.entrySet().stream()
//                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
//                .limit(limit)
//                .map(entry -> productCache.get(entry.getKey()))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Update product views (call this when a product is viewed)
//     *
//     * @param productId The ID of the viewed product
//     */
//    public void incrementProductViews(int productId) {
//        Product product = productCache.get(productId);
//        if (product != null) {
//            product.setViews(product.getViews() + 1);
//        }
//    }
//
//    /**
//     * Add search term to user's search history
//     *
//     * @param user The user
//     * @param searchTerm The search term
//     */
//    public void addSearchToHistory(User user, String searchTerm) {
//        if (user != null && searchTerm != null && !searchTerm.trim().isEmpty()) {
//            user.getSearchHistory().add(searchTerm.trim());
//        }
//    }
//
//    /**
//     * Add product to user's purchase history
//     *
//     * @param user The user
//     * @param productId The purchased product ID
//     */
//    public void addPurchaseToHistory(User user, int productId) {
//        if (user != null && productCache.containsKey(productId)) {
//            user.getPurchaseHistory().add(productId);
//        }
//    }
//
//    /**
//     * Add item to user's cart
//     *
//     * @param user The user
//     * @param productId The product ID
//     * @param quantity The quantity
//     * @param price The price at time of adding
//     * @param options Additional options
//     */
//    public void addToCart(User user, int productId, int quantity, float price, Map<String, String> options) {
//        if (user != null && productCache.containsKey(productId)) {
//            Product product = productCache.get(productId);
//            CartItem cartItem = new CartItem(user, product, quantity, price, options);
//            user.getCartItems().add(cartItem);
//            cartItems.add(cartItem);
//        }
//    }
//}