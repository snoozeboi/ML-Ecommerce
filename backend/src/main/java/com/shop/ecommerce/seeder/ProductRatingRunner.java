package com.shop.ecommerce.seeder;

import com.shop.ecommerce.messaging.dto.ProductEventType;
import com.shop.ecommerce.messaging.publisher.ProductEventPublisher;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * One-time runner to set random ratings (0.0 to 5.0) on all products.
 * Run with: --spring.profiles.active=init-ratings
 * (e.g. mvn spring-boot:run -Dspring-boot.run.profiles=init-ratings)
 */
@Profile("init-ratings")
@Component
@Order(3)
public class ProductRatingRunner implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;
    private final Random random = new Random();

    private static final float MIN_RATING = 0f;
    private static final float MAX_RATING = 5f;

    public ProductRatingRunner(ProductRepository productRepository, ProductEventPublisher productEventPublisher) {
        this.productRepository = productRepository;
      this.productEventPublisher = productEventPublisher;
    }

    @Override
    public void run(String... args) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            System.out.println("[ProductRatingInit] No products in database. Nothing to update.");
            return;
        }

        int count = 0;
        for (Product p : products) {
            p.setRating(nextRating());

            Product saved = productRepository.save(p);
            productEventPublisher.publish(ProductEventType.UPDATED, saved);

            count++;
        }
        System.out.println("[ProductRatingInit] Updated " + count + " products with random ratings (0.0 - 5.0).");
    }

    /** Returns a random rating in [0.0, 5.0] with one decimal place. */
    private float nextRating() {
        float r = MIN_RATING + random.nextFloat() * (MAX_RATING - MIN_RATING);
        return Math.round(r * 10f) / 10f;
    }
}
