package com.shop.ecommerce.config;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSegment;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



// This class creates a small seed to make admin user, the rest of the code is not used because we have a seeder class which has profile attached to it,
// So whenever we run the project for the first time, we activate the profile by "SeedingDB.txt" in the backend folder to seed Products,Users,Ratings for Products.


@Component
public class DataInitializer implements CommandLineRunner {

    private static final int PRODUCT_BATCH_SIZE = 500;
    private static final int USER_BATCH_SIZE = 200;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if it doesn't exist
        if (!userRepository.existsByEmail("admin@ecommerce.com")) {
            User adminUser = new User();
            adminUser.setUserName("admin");
            adminUser.setEmail("admin@ecommerce.com");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setGuest(false);
            adminUser.setSegment(UserSegment.UNCLASSIFIED);
            userRepository.save(adminUser);
            System.out.println("Admin user created: admin@ecommerce.com / admin123");
        }

        // Create regular user if it doesn't exist
        if (!userRepository.existsByEmail("user@ecommerce.com")) {
            User regularUser = new User();
            regularUser.setUserName("user");
            regularUser.setEmail("user@ecommerce.com");
            regularUser.setPasswordHash(passwordEncoder.encode("user123"));
            regularUser.setCreatedAt(LocalDateTime.now());
            regularUser.setGuest(false);
            regularUser.setSegment(UserSegment.UNCLASSIFIED);
            userRepository.save(regularUser);
            System.out.println("Regular user created: user@ecommerce.com / user123");
        }

        // Load products from seed CSV when table is empty
        // This ensures the full catalog (10,000 products) is available
        // for the frontend categories and browsing experience.
//        if (productRepository.count() == 0) {
//            loadProductsFromSeed();
//        }

        // Load users from seed CSV when table is empty
        // DISABLED - users have already been loaded into the database
        // if (userRepository.count() <= 2) {
        //     loadUsersFromSeed();
        // }
    }

    private void loadProductsFromSeed() {
        // We prefer the main project CSV at datasets/raw/products_10000.csv (with the latest images),
        // and fall back to the older classpath seed if that file is missing.
        try {
            Reader reader;

            // Try to resolve project root (go one level up when running from backend/)
            String userDir = System.getProperty("user.dir", ".");
            Path projectRoot = Paths.get(userDir);
            if (projectRoot.getFileName() != null &&
                    ("backend".equals(projectRoot.getFileName().toString())
                            || "backend\\".equals(projectRoot.getFileName().toString())
                            || "backend/".equals(projectRoot.getFileName().toString()))) {
                Path parent = projectRoot.getParent();
                if (parent != null) {
                    projectRoot = parent;
                }
            }

            Path datasetsCsv = projectRoot.resolve("datasets")
                    .resolve("raw")
                    .resolve("products_10000.csv");

            if (Files.exists(datasetsCsv)) {
                System.out.println("Seed: loading products from " + datasetsCsv.toAbsolutePath());
                reader = Files.newBufferedReader(datasetsCsv, StandardCharsets.UTF_8);
            } else {
                System.out.println("Seed: external CSV not found, falling back to classpath seed/products_10000.csv");
                ClassPathResource resource = new ClassPathResource("seed/products_10000.csv");
                reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            }

            try (reader;
                 CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setTrim(true)
                         .setIgnoreEmptyLines(true)
                         .build())) {

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");
                List<Product> batch = new ArrayList<>();
                int total = 0;

                for (CSVRecord record : parser) {
                    if (record.size() < 10) continue;
                    try {
                        String productName = getCsvValue(record, 1);
                        if (productName == null || productName.isBlank()) continue;

                        float price = parseFloatSafe(getCsvValue(record, 2), 0f);
                        int quantity = parseIntSafe(getCsvValue(record, 3), 0);
                        int views = parseIntSafe(getCsvValue(record, 4), 0);
                        String mainCategory = getCsvValue(record, 7);
                        String subCategory = getCsvValue(record, 8);
                        // Support both formats: with brand (cols 9=brand, 10=desc, 11=image) or without (9=desc, 10=image)
                        String brand = record.size() >= 12 ? getCsvValue(record, 9) : null;
                        String description = record.size() >= 12 ? getCsvValue(record, 10) : getCsvValue(record, 9);
                        String imageUrl = record.size() >= 12 ? getCsvValue(record, 11) : getCsvValue(record, 10);

                        LocalDateTime createdAt = parseProductDate(getCsvValue(record, 5), dateFormatter);
                        LocalDateTime updatedAt = parseProductDate(getCsvValue(record, 6), dateFormatter);

                        String brandValue = (brand != null && !brand.isBlank()) ? brand : null;
                        Product p = new Product(
                                productName,
                                description != null ? description : "",
                                mainCategory != null && !mainCategory.isBlank() ? mainCategory : "Unclassified",
                                brandValue,
                                price,
                                Math.max(0, quantity),
                                imageUrl != null ? imageUrl : "",
                                Math.max(0, views),
                                0f,
                                createdAt != null ? createdAt : LocalDateTime.now(),
                                updatedAt != null ? updatedAt : LocalDateTime.now(),
                                new ArrayList<>()
                        );
                        p.setSubCategory(subCategory != null && !subCategory.isBlank() ? subCategory : "Unclassified");
                        batch.add(p);

                        if (batch.size() >= PRODUCT_BATCH_SIZE) {
                            productRepository.saveAll(batch);
                            total += batch.size();
                            System.out.println("Seed: loaded " + total + " products...");
                            batch.clear();
                        }
                    } catch (Exception e) {
                        // skip bad row
                    }
                }
                if (!batch.isEmpty()) {
                    productRepository.saveAll(batch);
                    total += batch.size();
                }
                System.out.println("Seed: loaded " + total + " products from products_10000.csv");
            }
        } catch (Exception e) {
            System.err.println("Seed products load failed (using sample products): " + e.getMessage());
            createSampleProducts();
        }
    }

    private String getCsvValue(CSVRecord record, int index) {
        try {
            String v = record.get(index);
            return v != null ? v.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseProductDate(String value, DateTimeFormatter formatter) {
        if (value == null || value.isBlank()) return LocalDateTime.now();
        try {
            LocalDate d = LocalDate.parse(value.trim(), formatter);
            return d.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private float parseFloatSafe(String value, float def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Float.parseFloat(value.trim().replace("\"", ""));
        } catch (Exception e) {
            return def;
        }
    }

    private int parseIntSafe(String value, int def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Integer.parseInt(value.trim().replace("\"", ""));
        } catch (Exception e) {
            return def;
        }
    }

    private void loadUsersFromSeed() {
        // Skip if users have already been loaded (check if we have more than just admin/test users)
        long currentUserCount = userRepository.count();
        if (currentUserCount > 10) {
            System.out.println("Users from CSV have already been loaded. Skipping CSV read.");
            return;
        }
        
        try {
            ClassPathResource resource = new ClassPathResource("seed/users_5000.csv");
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                 CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setTrim(true)
                         .setIgnoreEmptyLines(true)
                         .build())) {

                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                List<User> batch = new ArrayList<>();
                int total = 0;
                String adminEmail = "admin@ecommerce.com";
                String testEmail = "user@ecommerce.com";

                for (CSVRecord record : parser) {
                    try {
                        String email = record.get("email");
                        if (email == null || email.isBlank()) continue;
                        email = email.trim();
                        if (adminEmail.equalsIgnoreCase(email) || testEmail.equalsIgnoreCase(email)) continue;
                        if (userRepository.existsByEmail(email)) continue;

                        String userName = record.get("user_name");
                        if (userName == null || userName.isBlank()) userName = "user" + record.get("id");
                        userName = userName.trim();
                        String passwordHash = record.get("password_hash");
                        if (passwordHash == null || passwordHash.isBlank()) passwordHash = "pass123";

                        boolean isGuest = "true".equalsIgnoreCase(record.get("is_guest"));
                        LocalDateTime createdAt = parseUserDate(record.get("created_at"), dateTimeFormatter);

                        User u = new User(userName, email, passwordEncoder.encode(passwordHash), createdAt);
                        u.setGuest(isGuest);
                        u.setSegment(UserSegment.UNCLASSIFIED);
                        batch.add(u);

                        if (batch.size() >= USER_BATCH_SIZE) {
                            userRepository.saveAll(batch);
                            total += batch.size();
                            System.out.println("Seed: loaded " + total + " users...");
                            batch.clear();
                        }
                    } catch (Exception e) {
                        // skip bad row
                    }
                }
                if (!batch.isEmpty()) {
                    userRepository.saveAll(batch);
                    total += batch.size();
                }
                System.out.println("Seed: loaded " + total + " users from users_5000.csv");
            }
        } catch (Exception e) {
            System.err.println("Seed users load failed: " + e.getMessage());
        }
    }

    private LocalDateTime parseUserDate(String value, DateTimeFormatter formatter) {
        if (value == null || value.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(value.trim(), formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void createSampleProducts() {
        List<Product> products = Arrays.asList(
                new Product("iPhone 15 Pro", "Latest iPhone with advanced camera system", "Electronics", "Apple", 999.99f, 50, "https://example.com/iphone15.jpg", 0, 0f, LocalDateTime.now(), LocalDateTime.now(), Arrays.asList("smartphone", "apple", "camera")),
                new Product("MacBook Air M2", "Lightweight laptop with M2 chip", "Electronics", "Apple", 1199.99f, 30, "https://example.com/macbook-air.jpg", 0, 0f, LocalDateTime.now(), LocalDateTime.now(), Arrays.asList("laptop", "apple", "m2")),
                new Product("Nike Air Max", "Comfortable running shoes", "Sports", "Nike", 129.99f, 100, "https://example.com/nike-airmax.jpg", 0, 0f, LocalDateTime.now(), LocalDateTime.now(), Arrays.asList("shoes", "running", "nike")),
                new Product("Coffee Maker", "Automatic coffee machine", "Home & Kitchen", "Nestle", 89.99f, 75, "https://example.com/coffee-maker.jpg", 0, 0f, LocalDateTime.now(), LocalDateTime.now(), Arrays.asList("coffee", "kitchen", "appliance")),
                new Product("Wireless Headphones", "Bluetooth noise-canceling headphones", "Electronics", "Samsung", 199.99f, 60, "https://example.com/headphones.jpg", 0, 0f, LocalDateTime.now(), LocalDateTime.now(), Arrays.asList("audio", "bluetooth", "wireless"))
        );
        productRepository.saveAll(products);
        System.out.println("Sample products created (5)");
    }
}
