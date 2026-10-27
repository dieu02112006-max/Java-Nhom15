package com.example.demo.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Ensures task-specific fields exist before the To-do API starts serving requests. */
@Component
public class TodoTaskSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public TodoTaskSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> columns = new HashSet<>(jdbc.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks'
                """, String.class));
        if (!columns.contains("category")) {
            jdbc.execute("ALTER TABLE `tasks` ADD COLUMN `category` varchar(80) NOT NULL DEFAULT 'Cá nhân' AFTER `description`");
        }
        if (!columns.contains("location")) {
            jdbc.execute("ALTER TABLE `tasks` ADD COLUMN `location` varchar(500) DEFAULT NULL AFTER `category`");
        }
    }
}
