package com.shop.ecommerce.seeder;

import com.shop.ecommerce.messaging.publisher.ProductEventPublisher;
import com.shop.ecommerce.messaging.dto.ProductEventType;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.ProductRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import lombok.RequiredArgsConstructor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;


@Profile("seed")
@Component
@RequiredArgsConstructor
@Order(1)
public class ProductSeedRunner implements CommandLineRunner {

  private final ProductRepository productRepository;
  private final ProductEventPublisher productEventPublisher;

  private static final DateTimeFormatter CSV_DT =
          DateTimeFormatter.ofPattern("d/M/uuuu");

  @Override
  public void run(String... args) throws Exception {

    if (productRepository.count() > 0) {
      System.out.println(">>> ProductSeedRunner skipped: products table already has data (" + productRepository.count() + " rows). Run only when DB is empty.");
      return;
    }

    ClassPathResource resource = new ClassPathResource("seed/products.csv");
    System.out.println(">>> ProductSeedRunner starting");
    try (
            InputStreamReader reader =
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            CSVParser csv = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreSurroundingSpaces()
                    .withTrim()
                    .parse(reader)
    ) {

      int count = 0;

      for (CSVRecord r : csv) {

        Product p = new Product();

        p.setProductName(r.get("product_name"));
        p.setPrice(Float.parseFloat(r.get("price")));
        p.setQuantity(Integer.parseInt(r.get("quantity")));
        p.setViews(Integer.parseInt(r.get("views")));

        p.setCreatedAt(LocalDate.parse(r.get("created_at"), CSV_DT).atStartOfDay());
        p.setUpdatedAt(LocalDate.parse(r.get("updated_at"), CSV_DT).atStartOfDay());

        p.setCategory(r.get("main_category"));
        p.setDescription(r.get("description"));
        p.setImageUrl(r.get("image_url"));

        // defaults / optional fields
        p.setRating(0f);
        p.setBrand(
                csv.getHeaderMap().containsKey("brand") ? r.get("brand") : "UNKNOWN"
        );
        p.setTags(List.of(p.getCategory().toLowerCase()));

        Product saved = productRepository.save(p);
        productEventPublisher.publish(ProductEventType.CREATED, saved);

        count++;
      }

      System.out.println("Seeded products: " + count);
    }
  }
}