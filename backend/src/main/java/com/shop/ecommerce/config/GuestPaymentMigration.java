package com.shop.ecommerce.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Makes user_id column nullable in payments table to support guest checkout.
 * Runs after the app is ready; safe to run multiple times.
 */
@Component
public class GuestPaymentMigration {

    private final JdbcTemplate jdbcTemplate;

    public GuestPaymentMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(4)
    public void makeUserIdNullable() {
        try {
            // PostgreSQL: ALTER COLUMN to allow NULL values
            // First check if column is already nullable by trying to set a constraint
            // If it fails, it means it's already nullable or the constraint doesn't exist
            jdbcTemplate.execute(
                "ALTER TABLE payments ALTER COLUMN user_id DROP NOT NULL"
            );
            System.out.println("Guest Payment migration: user_id column is now nullable.");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Column might already be nullable or constraint doesn't exist
            if (msg.contains("does not exist") || msg.contains("already") || msg.contains("constraint")) {
                System.out.println("Guest Payment migration: user_id column is already nullable or constraint doesn't exist.");
            } else {
                System.err.println("Guest Payment migration failed: " + msg);
                System.err.println("You may need to run manually: ALTER TABLE payments ALTER COLUMN user_id DROP NOT NULL;");
            }
        }
    }
}
