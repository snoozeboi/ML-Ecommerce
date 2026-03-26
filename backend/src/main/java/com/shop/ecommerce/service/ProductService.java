package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.ProductSuggestionDto;
import com.shop.ecommerce.dto.ProductUpsertRequest;
import com.shop.ecommerce.messaging.publisher.ProductEventPublisher;
import com.shop.ecommerce.messaging.dto.ProductEventType;
import com.shop.ecommerce.model.Discount;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.CartItemRepository;
import com.shop.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Service containing business logic for products. */
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductEventPublisher productEventPublisher;
  private final CartItemRepository cartItemRepository;
  private final MLService mlService;

  @Value("${ml.categorize.on.save:true}")
  private boolean mlCategorizeOnSave;

  public ProductService(ProductRepository productRepository, ProductEventPublisher productEventPublisher, CartItemRepository cartItemRepository, MLService mlService) {
    this.productRepository = productRepository;
    this.productEventPublisher = productEventPublisher;
    this.cartItemRepository = cartItemRepository;
    this.mlService = mlService;
  }

  private Product saveProduct(Product p) {
    return productRepository.save(p);
  }

  public Product create(ProductUpsertRequest request) {
    LocalDateTime now = LocalDateTime.now();

    String productName = "";
    if (request.getProductName() != null) {
      productName = request.getProductName().trim();
    }
    String description = "";
    if (request.getDescription() != null) {
      description = request.getDescription();
    }

    String category = "Unclassified";
    if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
      category = request.getCategory();
    }
    String subCategory = "";
    if (request.getSubCategory() != null && !request.getSubCategory().trim().isEmpty()) {
      subCategory = request.getSubCategory();
    }

    Product p = new Product();
    p.setProductName(productName);
    p.setDescription(description);
    p.setCategory(category);
    p.setSubCategory(subCategory);
    p.setBrand(request.getBrand());
    p.setPrice(request.getPrice());
    p.setQuantity(request.getQuantity());
    p.setImageUrl(request.getImageUrl());
    p.setViews(request.getViews());
    p.setRating(request.getRating());
    p.setCreatedAt(now);
    p.setUpdatedAt(now);
    List<String> tags;
    if (request.getTags() == null) {
      tags = new ArrayList<String>();
    } else {
      tags = request.getTags();
    }
    p.setTags(tags);

    Product created = saveProduct(p);
    try {
      productEventPublisher.publish(ProductEventType.CREATED, created);
    } catch (Exception e) {
      System.err.println("Product created but event publish failed (RabbitMQ may be down): " + e.getMessage());
    }
    runMlCategorizationAsync(created.getId());
    return created;
  }

  private void runMlCategorizationAsync(final int productId) {
    if (!mlCategorizeOnSave) {
      return;
    }
    Thread t = new Thread(() -> {
      try {
        System.out.println("[ML] Running categorization for product ID: " + productId);
        Map<String, Object> result = mlService.categorizeSingleProduct(productId);
        if (Boolean.TRUE.equals(result.get("success"))) {
          System.out.println("[ML] Product " + productId + " categorized: " + result.get("main_category") + " / " + result.get("sub_category"));
        } else {
          System.err.println("[ML] Product " + productId + " categorization failed: " + result.get("error"));
          if (result.get("output") != null && !result.get("output").toString().trim().isEmpty()) {
            System.err.println("[ML] Python output: " + result.get("output"));
          }
        }
      } catch (Exception ex) {
        System.err.println("[ML] Error running categorization for product " + productId + ": " + ex.getMessage());
        ex.printStackTrace();
      }
    });
    t.setName("ml-categorize-product-" + productId);
    t.start();
  }

  public Product update(int id, ProductUpsertRequest req) {
    Product p = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));

    String category = req.getCategory() != null ? req.getCategory().trim() : "";
    String subCategory = req.getSubCategory() != null ? req.getSubCategory().trim() : "";
    if (category.isEmpty()) category = "Unclassified";
    // sub_category: DB NOT NULL, keep empty string if not provided

    p.setProductName(req.getProductName() != null ? req.getProductName().trim() : "");
    p.setDescription(req.getDescription() != null ? req.getDescription() : "");
    p.setCategory(category);
    p.setSubCategory(subCategory);
    p.setBrand(req.getBrand());
    p.setPrice(req.getPrice());
    p.setQuantity(req.getQuantity());
    p.setImageUrl(req.getImageUrl());
    p.setTags(req.getTags() == null ? new ArrayList<String>() : req.getTags());
    p.setUpdatedAt(LocalDateTime.now());
    p.setRating(req.getRating());
    p.setViews(req.getViews());

    Product updated = productRepository.save(p);
    try {
      productEventPublisher.publish(ProductEventType.UPDATED, updated);
    } catch (Exception e) {
      System.err.println("Product updated but event publish failed (RabbitMQ may be down): " + e.getMessage());
    }
    boolean needsMl = category.isEmpty() || "Unclassified".equalsIgnoreCase(category) || subCategory.isEmpty();
    if (needsMl && mlCategorizeOnSave) {
      runMlCategorizationAsync(updated.getId());
    }
    return updated;
  }

  @Transactional
  public void delete(int id) {
    Product p = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));

    // Delete related cart items first to avoid foreign key constraint violation
    // This is safe even if no cart items exist
    try {
      cartItemRepository.deleteByProductId(id);
    } catch (Exception e) {
      // Log but don't fail - cart items might not exist
      System.err.println("Warning: Error deleting cart items for product " + id + ": " + e.getMessage());
    }

    // Publish delete event before actual deletion
    productEventPublisher.publish(ProductEventType.DELETED, p);
    
    // Delete the product
    productRepository.deleteById(id);
  }

  public Product getById(int id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
  }

  public Page<Product> getAllProducts(Pageable pageable) {
    return productRepository.findAll(pageable);
  }

  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  public Page<Product> searchProducts(String query, Pageable pageable) {
    return productRepository.searchProducts(query, pageable);
  }

  public ProductSuggestionDto toSuggestion(Product p) {
    float price = p.getPrice();
    Discount d = p.getDiscount();

    if (d == null || !d.isActiveNow(LocalDateTime.now())) {
      return new ProductSuggestionDto(
              p.getId(),
              p.getProductName(),
              p.getCategory(),
              p.getRating(),
              price,
              price,
              null,
              p.getImageUrl()
      );
    }

    int percent = d.getPercent();
    float finalPrice = price * (100 - percent) / 100f;

    return new ProductSuggestionDto(
            p.getId(),
            p.getProductName(),
            p.getCategory(),
            p.getRating(),
            price,
            finalPrice,
            percent,
            p.getImageUrl()
    );
  }

  /**
   * Import products from CSV file
   * Expected CSV format: product_name,description,category,sub_category,brand,price,quantity,image_url,views,rating,tags
   * Tags should be comma-separated within the field (e.g., "tag1,tag2,tag3")
   * 
   * @param file CSV file containing products
   * @return Import result with success count and errors
   */
  public Map<String, Object> importProductsFromCsv(MultipartFile file) {
    Map<String, Object> result = new HashMap<>();
    List<String> errors = new ArrayList<>();
    int successCount = 0;
    int totalRows = 0;

    try {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
      );

      String line;
      boolean isFirstLine = true;
      List<String> headers = new ArrayList<>();

      while ((line = reader.readLine()) != null) {
        totalRows++;
        
        // Skip empty lines
        if (line.trim().isEmpty()) {
          continue;
        }

        // Parse CSV line (handling quoted fields)
        List<String> values = parseCsvLine(line);

        if (isFirstLine) {
          // First line is header
          headers = values;
          isFirstLine = false;
          continue;
        }

        try {
          // Build a normalized header -> value map for easier access
          Map<String, String> rowMap = buildRowMap(values, headers);

          // Try to read product ID from CSV (if present)
          int csvId = parseInt(getValue(rowMap, "id", ""), -1);

          Product product;
          boolean isUpdate = false;

          if (csvId > 0 && productRepository.existsById(csvId)) {
            // Update existing product
            product = productRepository.findById(csvId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + csvId));
            applyRowMapToProduct(rowMap, product, false);
            isUpdate = true;
          } else {
            // Create new product
            product = mapRowMapToNewProduct(rowMap);
          }

          Product saved = productRepository.save(product);
          productEventPublisher.publish(isUpdate ? ProductEventType.UPDATED : ProductEventType.CREATED, saved);
          successCount++;
        } catch (Exception e) {
          errors.add("Row " + totalRows + ": " + e.getMessage());
        }
      }

      reader.close();

      result.put("success", true);
      result.put("message", "Import completed");
      result.put("totalRows", totalRows - 1); // Exclude header
      result.put("successCount", successCount);
      result.put("errorCount", errors.size());
      result.put("errors", errors);

    } catch (Exception e) {
      result.put("success", false);
      result.put("message", "Failed to import CSV: " + e.getMessage());
      result.put("errors", errors);
    }

    return result;
  }

  /**
   * Parse a CSV line, handling quoted fields
   */
  private List<String> parseCsvLine(String line) {
    List<String> values = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder currentValue = new StringBuilder();

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          // Escaped quote
          currentValue.append('"');
          i++; // Skip next quote
        } else {
          // Toggle quote state
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        // End of field
        values.add(currentValue.toString().trim());
        currentValue = new StringBuilder();
      } else {
        currentValue.append(c);
      }
    }
    
    // Add last field
    values.add(currentValue.toString().trim());

    return values;
  }

  /**
   * Map CSV row values to Product entity
   */
  private Product mapCsvRowToProduct(List<String> values, List<String> headers) {
    Map<String, String> rowMap = buildRowMap(values, headers);
    return mapRowMapToNewProduct(rowMap);
  }

  /**
   * Build a normalized header -> value map for a CSV row
   */
  private Map<String, String> buildRowMap(List<String> values, List<String> headers) {
    Map<String, String> rowMap = new HashMap<>();
    for (int i = 0; i < Math.min(headers.size(), values.size()); i++) {
      String header = headers.get(i).toLowerCase().replaceAll("[^a-z0-9_]", "");
      rowMap.put(header, values.get(i));
    }
    return rowMap;
  }

  /**
   * Create a new Product from a row map
   */
  private Product mapRowMapToNewProduct(Map<String, String> rowMap) {
    LocalDateTime now = LocalDateTime.now();
    Product product = new Product();
    applyRowMapToProduct(rowMap, product, true);
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    return product;
  }

  /**
   * Apply CSV row values to an existing or new Product
   */
  private void applyRowMapToProduct(Map<String, String> rowMap, Product product, boolean isNew) {
    LocalDateTime now = LocalDateTime.now();

    // Extract values with defaults
    String productName = getValue(rowMap, "productname", "product_name", "");
    String description = getValue(rowMap, "description", "");
    String category = getValue(rowMap, "category", "maincategory", "main_category", "Unclassified");
    String subCategory = getValue(rowMap, "subcategory", "sub_category", "Unclassified");
    String brand = getValue(rowMap, "brand", "");
    float price = parseFloat(getValue(rowMap, "price", "0"), 0f);
    int quantity = parseInt(getValue(rowMap, "quantity", "0"), 0);
    String imageUrl = getValue(rowMap, "imageurl", "image_url", "");
    int views = parseInt(getValue(rowMap, "views", "0"), 0);
    float rating = parseFloat(getValue(rowMap, "rating", "0"), 0f);

    // Parse tags (comma-separated in CSV)
    List<String> tags = new ArrayList<>();
    String tagsStr = getValue(rowMap, "tags", "");
    if (!tagsStr.isEmpty()) {
      String[] tagArray = tagsStr.split(",");
      for (String tag : tagArray) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty()) {
          tags.add(trimmed);
        }
      }
    }

    // Validate required fields
    if (productName.isEmpty()) {
      throw new IllegalArgumentException("Product name is required");
    }
    if (category.isEmpty()) {
      category = "Unclassified";
    }

    // Apply fields
    product.setProductName(productName);
    product.setDescription(description);
    product.setCategory(category);
    product.setSubCategory(subCategory);
    product.setBrand(brand);
    product.setPrice(price);
    product.setQuantity(quantity);
    product.setImageUrl(imageUrl);
    product.setViews(views);
    product.setRating(rating);
    product.setTags(tags);

    // Timestamps
    if (isNew || product.getCreatedAt() == null) {
      product.setCreatedAt(now);
    }
    product.setUpdatedAt(now);
  }

  private String getValue(Map<String, String> rowMap, String... keys) {
    for (String key : keys) {
      if (rowMap.containsKey(key) && rowMap.get(key) != null) {
        String value = rowMap.get(key);
        // Remove quotes if present
        if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
        }
        return value.trim();
      }
    }
    return "";
  }

  private float parseFloat(String value, float defaultValue) {
    try {
      if (value == null || value.trim().isEmpty()) {
        return defaultValue;
      }
      // Remove quotes and parse
      value = value.replace("\"", "").trim();
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private int parseInt(String value, int defaultValue) {
    try {
      if (value == null || value.trim().isEmpty()) {
        return defaultValue;
      }
      // Remove quotes and parse
      value = value.replace("\"", "").trim();
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public Page<Product> getProductsFiltered(
          String category,
          Float minPrice,
          Float maxPrice,
          Pageable pageable
  ) {
    return productRepository.findFiltered(category, minPrice, maxPrice, pageable);
  }

  public Page<Product> searchFiltered(
          String search,
          String category,
          Float minPrice,
          Float maxPrice,
          Pageable pageable
  ) {
    return productRepository.searchFiltered(search, category, minPrice, maxPrice, pageable);
  }
}