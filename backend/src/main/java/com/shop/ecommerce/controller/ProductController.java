package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ProductSummaryDto;
import com.shop.ecommerce.dto.ProductUpsertRequest;
import com.shop.ecommerce.dto.SuggestResponseDto;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.service.AuthService;
import com.shop.ecommerce.service.ProductService;
import com.shop.ecommerce.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// REST controller handling HTTP requests.
/// Delegates all logic to ProductService and returns DTOs as responses.

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);

  private final ProductService productService;
  private final ProductSearchService productSearchService;
  private final ProductRepository productRepository;


  @Autowired
  private AuthService authService;

  public ProductController(ProductService productService, ProductSearchService productSearchService, ProductRepository productRepository) {
    this.productService = productService;
    this.productSearchService = productSearchService;
    this.productRepository = productRepository;
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody ProductUpsertRequest req,
                                  @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    // Check if user is admin
    if (userEmail == null || !authService.isAdmin(userEmail)) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", "Unauthorized: Admin access required");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      Product product = productService.create(req);
      return ResponseEntity.ok(product);
    } catch (Exception e) {
      log.error("Failed to create product", e);
      Map<String, Object> error = new HashMap<>();
      String msg = e.getMessage() != null ? e.getMessage() : "Failed to create product";
      error.put("success", false);
      error.put("message", msg);
      error.put("errorType", e.getClass().getSimpleName());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable int id,
                                  @RequestBody ProductUpsertRequest req,
                                  @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    // Check if user is admin
    if (userEmail == null || !authService.isAdmin(userEmail)) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", "Unauthorized: Admin access required");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      Product product = productService.update(id, req);
      return ResponseEntity.ok(product);
    } catch (RuntimeException e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", e.getMessage() != null ? e.getMessage() : "Failed to update product");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    } catch (Exception e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", "An error occurred while updating the product: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable int id,
                                  @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    // Check if user is admin
    if (userEmail == null || !authService.isAdmin(userEmail)) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", "Unauthorized: Admin access required");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      productService.delete(id);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Product deleted successfully");
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", e.getMessage() != null ? e.getMessage() : "Failed to delete product");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    } catch (Exception e) {
      Map<String, Object> error = new HashMap<>();
      error.put("success", false);
      error.put("message", "An error occurred while deleting the product: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  @GetMapping("/list")
  public ResponseEntity<?> getAllProducts(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "50") int size,
          @RequestParam(required = false) String search,
          @RequestParam(required = false) String category,
          @RequestParam(required = false) Float minPrice,
          @RequestParam(required = false) Float maxPrice,
          @RequestParam(defaultValue = "NAME_ASC") String sort
  ) {
    if (page < 0) page = 0;
    if (size < 1) size = 50;
    if (size > 200) size = 200;

    // map UI sort → DB sort
    Sort springSort = switch (sort.toUpperCase()) {
      case "NAME_DESC"   -> Sort.by("productName").descending();
      case "PRICE_ASC"   -> Sort.by("price").ascending();
      case "PRICE_DESC"  -> Sort.by("price").descending();
      case "RATING_DESC" -> Sort.by("rating").descending();
      default            -> Sort.by("rating").ascending(); // NAME_ASC
    };

    Pageable pageable = PageRequest.of(page, size, springSort);

    Page<Product> productPage;
    String searchText = (search == null) ? "" : search.trim();

    if (!searchText.isEmpty()) {
      productPage = productService.searchFiltered(
              searchText, category, minPrice, maxPrice, pageable
      );
    } else {
      productPage = productService.getProductsFiltered(
              category, minPrice, maxPrice, pageable
      );
    }

    // ✅ entity → DTO mapping
    List<ProductSummaryDto> content = productPage.getContent().stream()
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

    Map<String, Object> response = new HashMap<>();
    response.put("content", content);
    response.put("totalElements", productPage.getTotalElements());
    response.put("totalPages", productPage.getTotalPages());
    response.put("currentPage", productPage.getNumber());
    response.put("pageSize", productPage.getSize());
    response.put("hasNext", productPage.hasNext());
    response.put("hasPrevious", productPage.hasPrevious());

    return ResponseEntity.ok(response);
  }

  /** All distinct categories and subcategories in the DB (for admin edit dropdowns). */
  @GetMapping("/categories")
  public Map<String, List<String>> getCategories() {
    List<String> categories = productRepository.findDistinctCategories();
    List<String> subcategories = productRepository.findDistinctSubCategories();
    return Map.of("categories", categories, "subcategories", subcategories);
  }

@GetMapping("/{id:\\d+}")
public Product get(@PathVariable int id) {
  // GET endpoint doesn't require admin access
  // The regex \\d+ ensures only numeric IDs match this endpoint
  return productService.getById(id);
}

@PostMapping("/import")
public ResponseEntity<?> importFromCsv(
        @RequestParam("file") MultipartFile file,
        @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
  // Check if user is admin
  if (userEmail == null || !authService.isAdmin(userEmail)) {
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", "Unauthorized: Admin access required");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  // Validate file
  if (file.isEmpty()) {
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", "File is empty");
    return ResponseEntity.badRequest().body(error);
  }

  if (!file.getOriginalFilename().endsWith(".csv")) {
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", "File must be a CSV file");
    return ResponseEntity.badRequest().body(error);
  }

  Map<String, Object> result = productService.importProductsFromCsv(file);
  return ResponseEntity.ok(result);
}

@GetMapping("/suggest")
public SuggestResponseDto suggest(
        @RequestParam(name = "q") String q,
        @RequestParam(name = "category", required = false) String category
) {
  return productSearchService.suggest(q, category);
}

@GetMapping("/category/{category}/max-price")
public Map<String, Object> getMaxPriceByCategory(@PathVariable String category) {
  float max = productRepository.findMaxPriceByCategory(category);
  int maxInt = (int) Math.ceil(max); // so 199.01 -> 200
  return Map.of(
          "category", category,
          "maxPrice", maxInt
  );
}

}