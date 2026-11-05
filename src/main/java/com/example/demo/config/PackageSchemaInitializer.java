package com.example.demo.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds package entitlements to existing users and initializes the three sample accounts once. */
@Component
public class PackageSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public PackageSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS package_payments (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    txn_ref VARCHAR(40) NOT NULL UNIQUE,
                    user_id BIGINT NOT NULL,
                    plan_code VARCHAR(20) NOT NULL,
                    amount_vnd BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    review_token_hash VARCHAR(64) NULL,
                    review_expires_at DATETIME NULL,
                    created_at DATETIME NOT NULL,
                    paid_at DATETIME NULL,
                    INDEX idx_package_payments_user (user_id, created_at),
                    CONSTRAINT fk_package_payments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
        List<String> columns = jdbc.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
                """, String.class);
        if (!columns.contains("subscription_tier")) {
            jdbc.execute("ALTER TABLE users ADD COLUMN subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE'");
            jdbc.update("UPDATE users SET subscription_tier = 'PLUS' WHERE LOWER(full_name) = LOWER(?)", "Nguyễn Văn B");
            jdbc.update("UPDATE users SET subscription_tier = 'PRO' WHERE LOWER(full_name) = LOWER(?)", "Nguyễn Văn C");
        }

        List<String> paymentColumns = jdbc.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'package_payments'
                """, String.class);
        if (!paymentColumns.contains("review_token_hash")) {
            jdbc.execute("ALTER TABLE package_payments ADD COLUMN review_token_hash VARCHAR(64) NULL");
        }
        if (!paymentColumns.contains("review_expires_at")) {
            jdbc.execute("ALTER TABLE package_payments ADD COLUMN review_expires_at DATETIME NULL");
        }
    }
}
