package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/// Repository responsible for fetching Product entities from the database.
/// Used only by the service layer.

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(String category);
    List<Product> findByProductNameContainingIgnoreCase(String productName);
    List<Product> findByPriceBetween(float minPrice, float maxPrice);
    List<Product> findAllByOrderByViewsDesc(Pageable pageable);
    List<Product> findAllByOrderByRatingDesc(Pageable pageable);

    @Query("select coalesce(max(p.price), 0) from Product p where (:category is null or :category = 'all' or lower(p.category) = lower(:category))")
    float findMaxPriceByCategory(@Param("category") String category);

    // Search across multiple fields
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.tags t " +
           "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.subCategory) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("""
            select p from Product p
            where (:category is null or :category = '' or lower(p.category) = lower(:category))
            and (:minPrice is null or p.price >= :minPrice)
            and (:maxPrice is null or p.price <= :maxPrice)
            """)
    Page<Product> findFiltered(
            @Param("category") String category,
            @Param("minPrice") Float minPrice,
            @Param("maxPrice") Float maxPrice,
            Pageable pageable
    );

    @Query("""
            select p from Product p
            where (
            lower(p.productName) like lower(concat('%', :search, '%'))
            or lower(p.description) like lower(concat('%', :search, '%'))
            )
            and (:category is null or :category = '' or lower(p.category) = lower(:category))
            and (:minPrice is null or p.price >= :minPrice)
            and (:maxPrice is null or p.price <= :maxPrice)
            """)
    Page<Product> searchFiltered(
            String search,
            String category,
            Float minPrice,
            Float maxPrice,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.category != '' ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.subCategory FROM Product p WHERE p.subCategory IS NOT NULL AND p.subCategory != '' ORDER BY p.subCategory")
    List<String> findDistinctSubCategories();
} 