package com.shop.ecommerce.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds ml_category column to users table on first run (no psql needed).
 * Runs after the app is ready; safe to run multiple times.
 */
@Component
public class MLCategoryMigration {

    private final JdbcTemplate jdbcTemplate;

    public MLCategoryMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    public void addMLCategoryColumn() {
        try {
            // PostgreSQL: ADD COLUMN IF NOT EXISTS (9.5+)
            jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS ml_category VARCHAR(255)"
            );
            System.out.println("ML Category migration: ml_category column ensured.");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("already exists") || msg.contains("duplicate column")) {
                System.out.println("ML Category migration: ml_category column already present.");
                return;
            }
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN ml_category VARCHAR(255)");
                System.out.println("ML Category migration: ml_category column added.");
            } catch (Exception e2) {
                if (e2.getMessage() != null && (e2.getMessage().contains("already exists") || e2.getMessage().contains("duplicate"))) {
                    System.out.println("ML Category migration: ml_category column already present.");
                } else {
                    System.err.println("ML Category migration: ml_category failed. Add manually: ALTER TABLE users ADD COLUMN ml_category VARCHAR(255);");
                }
            }
        }
    }
}
