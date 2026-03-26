# Flow Diagrams - E-Commerce Recommendation System

## 📦 Flow Diagram: Product Categorization

```
1. Load Data
   ↓
   - products_10000.csv
   ↓
2. Clean Data
   ↓
   - Fill missing fields (name/description → '', categories → 'Unknown')
   - Remove completely empty products
   ↓
3. Prepare Features
   ↓
   - Combine text: product_name + description (doubled) → combined_text
   - Create combined category: main_category || sub_category
   - Separate: X_text, X_price, y (categories)
   ↓
4. Split Data
   ↓
   - 80% training, 20% testing
   - Stratified split (maintain proportions)
   ↓
5. Convert Text to Numbers (TF-IDF)
   ↓
   - TF-IDF Vectorization (1200 features, n-grams 1-2)
   - Normalize price (StandardScaler)
   - Combine features: TF-IDF + normalized price
   ↓
6. Train XGBoost Model
   ↓
   - Label Encoding (categories → numbers)
   - XGBoost Classifier (60 trees, max_depth=5, learning_rate=0.15)
   - Train on training set
   ↓
7. Evaluation
   ↓
   - Predict on test set
   - Calculate Accuracy (Combined, Main, Sub)
   ↓
8. Categorize All Products
   ↓
   - Predict for all products
   - Add categories: predicted_category, predicted_main_category, predicted_sub_category
   ↓
9. Save Results
   ↓
   - products_with_categories.csv
   - Metrics: Accuracy, Precision, Recall
```

---

## 👥 Flow Diagram: User Categorization

```
1. Load Data
   ↓
   - products_10000.csv
   - users_5000.csv
   - user_clicks_interactions.csv (Wide → Long)
   - user_purchase_interactions.csv (Wide → Long)
   - user_visits_time_interactions.csv (Wide → Long)
   ↓
2. Prepare User Features
   ↓
   - Calculate 35 features per user:
     * Basic features: total_clicks, total_purchases, total_visit_time, unique_products
     * Derived features: conversion_rate, category_diversity, avg_price, clicks_per_product
     * Behavioral features: engagement_score, activity_intensity, purchase_frequency
     * Advanced features: purchase_velocity, exploration_ratio, value_per_interaction
     * Time features: days_since_registration
     * Category features: favorite_category_hash
   ↓
3. Normalize Features
   ↓
   - RobustScaler (handle outliers)
   - StandardScaler (normalize to uniform scale)
   ↓
4. Create User Categories (Target Variable)
   ↓
   - Categories based on behavior patterns:
     * inactive, high_value, price_sensitive, occasional_buyer
     * explorer, active_browser, category_loyal, light_user
   - Label Encoding (categories → numbers)
   ↓
5. Feature Selection
   ↓
   - SelectKBest with Mutual Information
   - Select top 20 most important features
   ↓
6. Split Data
   ↓
   - 80% training, 20% testing
   - Stratified split (maintain proportions)
   ↓
7. Hyperparameter Tuning
   ↓
   - GridSearchCV (32 combinations × 3-fold CV = 96 model fits)
   - Parameters: n_estimators, max_depth, min_samples_split, min_samples_leaf, max_features
   - Scoring: F1-weighted (suitable for imbalanced classes)
   ↓
8. Train Random Forest Model
   ↓
   - Use best model from GridSearchCV
   - Random Forest (150-200 trees, class_weight='balanced')
   ↓
9. Evaluation
   ↓
   - Predict on test set
   - Calculate metrics: Accuracy, Precision, Recall, F1 Score
   - Classification Report for each category
   ↓
10. Predict for All Users
    ↓
    - Predict categories for all users
    - Add categories: cluster (number), category (name)
    ↓
11. Feature Importance
    ↓
    - Calculate feature importance
    - List of top 10 most important features
    ↓
12. Save Results
    ↓
    - users_with_clusters.csv
    - categorization_summary.csv
    - feature_importance.csv
    - best_parameters.json
```

---

## 🎯 Flow Diagram: Recommendation System (Phase 2)

```
1. Load Data
   ↓
   - products_10000.csv
   - users_5000.csv
   - user_clicks_interactions.csv (Wide → Long)
   - user_purchase_interactions.csv (Wide → Long)
   - user_visits_time_interactions.csv (Wide → Long)
   - users_with_clusters.csv (from Phase 1)
   ↓
2. Product Categorization
   ↓
   - Use ProductCategorization from Phase 1
   - Train XGBoost on products
   - Categorize all products → products_with_clusters
   ↓
3. Prepare TF-IDF for Products
   ↓
   - Use ProductCategorization.get_tfidf_matrix_for_descriptions()
   - Convert product descriptions to TF-IDF vectors (100 features)
   - Matrix: each row = product, each column = word/phrase
   ↓
4. Create User-Product Interaction Matrix
   ↓
   - Identify all users and products
   - Create mappings: user_id_to_index, product_id_to_index
   - Build weighted matrix:
     * Clicks: weight 1.0
     * Purchases: weight 5.0
     * Visit time: weight 0.1
   ↓
5. Calculate User Similarity
   ↓
   - Normalize interaction matrix (L2 normalization)
   - Calculate Cosine Similarity between all user pairs
   - Similarity matrix: user_similarity_matrix
   ↓
6. (Optional) Train Neural Network Ranking
   ↓
   - Prepare features (17 features):
     * user_cluster, product_cluster (from Phase 1!)
     * product_price, product_category
     * total_interactions, num_products
     * user_category_encoded, category_match (from Phase 1!)
     * user_similarity_score, product_popularity
     * ... and more
   - Build neural network (512→256→128→64 neurons)
   - Train with Early Stopping + Learning Rate Reduction
   ↓
7. Evaluate Recommendations
   ↓
   - For each user with interactions:
     ↓
     Check: new user or existing user?
     ↓
     New user (< 3 interactions)?
     ↓
     Yes → Recommendations for new user:
     - TF-IDF similarity (40%)
     - Category match (50%)
     - Popularity (10%)
     ↓
     No → Existing user → Hybrid recommendations:
     ↓
     Step 1: Collaborative Filtering
     - Find 25 similar users
     - Collect products that similar users liked
     - Score: similarity × interaction_value × weight_multiplier
     ↓
     Step 2: Content-Based Filtering
     - Find user's preferred categories
     - Find products from same categories
     - Score: category_weight × popularity × position_score
     ↓
     Step 3: Hybrid Combination
     - Collaborative: 45% weight
     - Content-Based: 55% weight
     - 25% bonus for products appearing in both
     ↓
     (Optional) Step 4: Neural Network Ranking
     - If Neural Network available and trained:
       * Take 10x more recommendations from hybrid
       * Rank each recommendation with Neural Network
       * Combine scores: Base (60%) + Neural (40%)
       * Return top N best
     - If not available:
       * Return hybrid recommendations only
     ↓
     Calculate Precision@K:
     - Check if recommendations are in categories user purchased
     - Bonuses: exact match, good category match
   ↓
8. Save Results
   ↓
   - recommendation_evaluation.csv
   - Metrics: Precision@K, Accuracy
   ↓
9. (Optional) Dynamic Updates + Continuous Learning
   ↓
   - Update new interaction:
     * update_interaction_dynamic() → update interaction matrix
     * recalculate_user_similarity() → recalculate similarity
   - Continuous Learning:
     * Track new interactions (new_interactions_count)
     * If 100+ new interactions:
       → check_and_retrain_neural_network()
       → Retrain Neural Network on new data
```

---

## 📊 Summary: Phase 1 + Phase 2

### Phase 1 - Categorization:
```
Products: Data → Clean → Features → TF-IDF → XGBoost → Categories
Users: Data → Features (35) → Normalize → Categories → Random Forest → Categories
```

### Phase 2 - Recommendations:
```
Data → TF-IDF + Matrices → New user? → TF-IDF+Category+Popularity
                          Existing user? → Collaborative+Content-Based → (Optional) Neural Network → Recommendations
```

---

## 🔗 Using Phase 1 Results in Phase 2:

```
Phase 1 (Categorization):
   ↓
   products_with_categories.csv
   users_with_clusters.csv
   ↓
Phase 2 (Recommendations):
   ↓
   Product categories → Content-Based Filtering
   User categories → Neural Network Ranking (features #0, #7, #8)
   ↓
   Improved recommendations!
```

---

## 📝 Important Notes:

1. **Product Categorization** uses **XGBoost** (60 trees, fast and accurate)
2. **User Categorization** uses **Random Forest** (150-200 trees, with GridSearchCV)
3. **Recommendation System** combines:
   - **Collaborative Filtering** (45%) - based on similar users
   - **Content-Based Filtering** (55%) - based on product categories
   - **Neural Network Ranking** (optional) - advanced ranking with 17 features
4. **Categories from Phase 1** are used in Neural Network:
   - `user_cluster` (feature #0)
   - `user_category_encoded` (feature #7)
   - `category_match` (feature #8)

---

*Created: 2024*
