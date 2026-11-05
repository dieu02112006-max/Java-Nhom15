package com.example.demo.config;

import java.sql.Statement;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds the administrator-only blocked task state to existing MySQL databases. */
@Component
public class TaskBlockedStatusSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public TaskBlockedStatusSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("mysql") || product.contains("mariadb")) {
                String columnType = jdbc.query("""
                        SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'status'
                        """, rs -> rs.next() ? rs.getString(1) : null);
                if (columnType != null && !columnType.contains("'blocked'")) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE tasks MODIFY COLUMN status ENUM('todo','in_progress','done','blocked') NOT NULL DEFAULT 'todo'");
                    }
                }
            }
            return null;
        });
    }
}
