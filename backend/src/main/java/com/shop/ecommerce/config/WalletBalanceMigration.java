package com.shop.ecommerce.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds wallet_balance column to users table on first run (no psql needed).
 * Runs after the app is ready; safe to run multiple times.
 */
@Component
public class WalletBalanceMigration {

    private final JdbcTemplate jdbcTemplate;

    public WalletBalanceMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void addWalletBalanceColumn() {
        try {
            // PostgreSQL: ADD COLUMN IF NOT EXISTS (9.5+)
            jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS wallet_balance DOUBLE PRECISION NOT NULL DEFAULT 0"
            );
            System.out.println("Wallet migration: wallet_balance column ensured.");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Column already exists (e.g. H2 or older PostgreSQL)
            if (msg.contains("already exists") || msg.contains("duplicate column")) {
                System.out.println("Wallet migration: wallet_balance column already present.");
                return;
            }
            // Fallback for DBs without IF NOT EXISTS: try plain ADD COLUMN
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN wallet_balance DOUBLE PRECISION NOT NULL DEFAULT 0"
                );
                System.out.println("Wallet migration: wallet_balance column added.");
            } catch (Exception e2) {
                if (e2.getMessage() != null && (e2.getMessage().contains("already exists") || e2.getMessage().contains("duplicate"))) {
                    System.out.println("Wallet migration: wallet_balance column already present.");
                } else {
                    System.err.println("Wallet migration failed. Add column manually: ALTER TABLE users ADD COLUMN wallet_balance DOUBLE PRECISION NOT NULL DEFAULT 0;");
                }
            }
        }
        addStripeCustomerIdColumn();
    }

    @Order(2)
    public void addStripeCustomerIdColumn() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255)"
            );
            System.out.println("Wallet migration: stripe_customer_id column ensured.");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("already exists") || msg.contains("duplicate column")) {
                System.out.println("Wallet migration: stripe_customer_id column already present.");
                return;
            }
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255)");
                System.out.println("Wallet migration: stripe_customer_id column added.");
            } catch (Exception e2) {
                if (e2.getMessage() != null && (e2.getMessage().contains("already exists") || e2.getMessage().contains("duplicate"))) {
                    System.out.println("Wallet migration: stripe_customer_id column already present.");
                } else {
                    System.err.println("Wallet migration: stripe_customer_id failed. Add manually: ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255);");
                }
            }
        }
    }
}
