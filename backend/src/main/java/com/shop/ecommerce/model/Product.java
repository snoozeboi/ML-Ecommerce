package com.shop.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;       // for date and time
import java.util.List;
import java.util.ArrayList;

/// JPA entity representing a product stored in the database.
/// Loaded by ProductRepository and used internally in services.
/// Never sent directly to the frontend.

@Entity
@Table(name = "products")
public class Product {

    // Product's Attributes
    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Getter
    @Setter
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Getter
    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Getter
    @Setter
    @Column(nullable = true)
    private String category;

    @Getter
    @Setter
    @Column(name = "sub_category", nullable = true)
    private String subCategory;

    /** ML-assigned category from Phase 1 (e.g. main category from product categorization). Filled by POST /api/ml/phase1 or single-product categorize. */
    @Getter
    @Setter
    @Column(name = "ml_category")
    private String mlCategory;

    @Getter
    @Setter
    @Column
    private String brand;

    @Getter
    @Setter
    @Column(nullable = false)
    private float price;

    @Getter
    @Setter
    @Column(nullable = false)
    private int quantity;

    @Getter
    @Setter
    @Column(name = "image_url")
    private String imageUrl;

    @Getter
    @Setter
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Column(nullable = false)
    private int views = 0;

    @Getter
    @Setter
    @Column(nullable = false)
    private float rating = 0f;

    @Getter
    @Setter
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Getter
    @Setter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags;

    // Default constructor required by JPA
    public Product() {
        this.views = 0;
        this.tags = new ArrayList<String>();
    }

    // Product's Constructor
    public Product(
            String productName,
            String description,
            String category,
            String brand,
            float price,
            int quantity,
            String imageUrl,
            int views,
            float rating,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<String> tags
    ) {
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.views = views;
        this.rating = rating;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

}