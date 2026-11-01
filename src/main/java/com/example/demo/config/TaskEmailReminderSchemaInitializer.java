package com.example.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskEmailReminderSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public TaskEmailReminderSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS task_email_reminders (
                    task_id BIGINT NOT NULL,
                    due_date DATETIME NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    claimed_at DATETIME NOT NULL,
                    sent_at DATETIME DEFAULT NULL,
                    PRIMARY KEY (task_id, due_date)
                )
                """);
    }
}
