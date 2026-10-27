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
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS todo_work_goals (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    title VARCHAR(120) NOT NULL,
                    created_at DATE NOT NULL,
                    CONSTRAINT fk_todo_work_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
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
        if (!columns.contains("goal_id")) {
            jdbc.execute("ALTER TABLE `tasks` ADD COLUMN `goal_id` bigint DEFAULT NULL AFTER `parent_task_id`");
            jdbc.execute("ALTER TABLE `tasks` ADD CONSTRAINT `fk_tasks_goal` FOREIGN KEY (`goal_id`) REFERENCES `todo_work_goals` (`id`) ON DELETE SET NULL");
        }
        Set<String> goalColumns = new HashSet<>(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'todo_work_goals'
                """, String.class));
        if (goalColumns.contains("target")) jdbc.execute("ALTER TABLE `todo_work_goals` MODIFY `target` int NULL");
        if (goalColumns.contains("deadline")) jdbc.execute("ALTER TABLE `todo_work_goals` MODIFY `deadline` date NULL");
    }
}
