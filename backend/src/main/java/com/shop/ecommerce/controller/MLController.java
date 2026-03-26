package com.shop.ecommerce.controller;

import com.shop.ecommerce.service.MLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for ML Algorithm Execution
 * 
 * Provides endpoints for:
 * - Running Phase 1: User and Product Categorization
 * - Running Phase 2: Recommendation System
 * - Running Phase 3: Single Item Categorization
 */
@RestController
@RequestMapping("/api/ml")
public class MLController {

    @Autowired
    private MLService mlService;

    /**
     * Run Phase 1: User and Product Categorization
     * 
     * This endpoint:
     * 1. Executes Python ML scripts for user and product categorization
     * 2. Reads results from datasets/results/phase1/
     * 3. Updates users in PostgreSQL with ML categories
     * 4. Updates products in PostgreSQL with ML categories
     * 
     * @return Execution results with detailed stage information
     */
    @PostMapping("/phase1")
    public ResponseEntity<Map<String, Object>> runPhase1() {
        try {
            Map<String, Object> results = mlService.runPhase1();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * One-time sync: apply product categories from datasets/results/phase1/products_with_categories.csv
     * to the database. Does not run Phase 1 scripts or export. Use once to fix DB from the correct CSV;
     * after that, single-product ML uses the demo catalog.
     * @param byRowOrder if true, CSV row 2 → DB product id 1, row 3 → id 2, etc. Use when CSV ids don't match DB.
     */
    @PostMapping("/sync-products-from-csv")
    public ResponseEntity<Map<String, Object>> syncProductCategoriesFromCsv(
            @RequestParam(required = false, defaultValue = "false") boolean byRowOrder) {
        try {
            Map<String, Object> results = mlService.syncProductCategoriesFromCsv(byRowOrder);
            if (Boolean.TRUE.equals(results.get("success"))) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(results);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Apply product categories from seed/products.csv to the database by row order.
     * DB product id 1 = first seed row, id 2 = second seed row, etc. Use this to make the DB match the seed.
     */
    @PostMapping("/sync-categories-from-seed")
    public ResponseEntity<Map<String, Object>> syncCategoriesFromSeed() {
        try {
            Map<String, Object> results = mlService.syncCategoriesFromSeed();
            if (Boolean.TRUE.equals(results.get("success"))) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(results);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Run Phase 2: Recommendation System
     * 
     * @return Execution results
     */
    @PostMapping("/phase2")
    public ResponseEntity<Map<String, Object>> runPhase2() {
        try {
            Map<String, Object> results = mlService.runPhase2();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Run Phase 3: Single Item Categorization
     * 
     * @return Execution results
     */
    @PostMapping("/phase3")
    public ResponseEntity<Map<String, Object>> runPhase3() {
        try {
            Map<String, Object> results = mlService.runPhase3();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Categorize a single user and update database
     * 
     * @param userId User ID to categorize
     * @return Categorization result with database update status
     */
    @PostMapping("/user/{userId}/categorize")
    public ResponseEntity<Map<String, Object>> categorizeUser(@PathVariable int userId) {
        try {
            Map<String, Object> results = mlService.categorizeSingleUser(userId);
            if (Boolean.TRUE.equals(results.get("success"))) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(results);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Categorize a single product and update database
     * 
     * @param productId Product ID to categorize
     * @return Categorization result with database update status
     */
    @PostMapping("/product/{productId}/categorize")
    public ResponseEntity<Map<String, Object>> categorizeProduct(@PathVariable int productId) {
        try {
            Map<String, Object> results = mlService.categorizeSingleProduct(productId);
            if (Boolean.TRUE.equals(results.get("success"))) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.badRequest().body(results);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get status of ML execution
     * 
     * @return Status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "ready",
            "phases_available", Map.of(
                "phase1", "User and Product Categorization",
                "phase2", "Recommendation System",
                "phase3", "Single Item Categorization"
            ),
            "endpoints", Map.of(
                "phase1", "POST /api/ml/phase1",
                "categorize_user", "POST /api/ml/user/{userId}/categorize",
                "categorize_product", "POST /api/ml/product/{productId}/categorize"
            )
        ));
    }
}
