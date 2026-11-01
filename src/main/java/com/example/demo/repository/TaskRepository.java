package com.example.demo.repository;

import com.example.demo.entity.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Task> mapper = (rs, row) -> new Task(
            rs.getLong("id"), rs.getLong("user_id"), rs.getLong("list_id"),
            rs.getObject("parent_task_id", Long.class), rs.getString("title"), rs.getString("description"),
            rs.getString("status"), rs.getString("priority"),
            rs.getTimestamp("due_date") == null ? null : rs.getTimestamp("due_date").toLocalDateTime(),
            rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
            rs.getBigDecimal("estimated_cost"), rs.getString("recurrence_rule"), rs.getInt("position"));

    public TaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Task> findAll() {
        return jdbc.query("SELECT id,user_id,list_id,parent_task_id,title,description,status,priority,due_date,completed_at,estimated_cost,recurrence_rule,position FROM tasks ORDER BY id", mapper);
    }
    public Optional<Task> findById(Long id) {
        return jdbc.query("SELECT id,user_id,list_id,parent_task_id,title,description,status,priority,due_date,completed_at,estimated_cost,recurrence_rule,position FROM tasks WHERE id=?", mapper, id).stream().findFirst();
    }
    public Task save(Task task) {
        if (task.getId() == null) {
            jdbc.update("INSERT INTO tasks (user_id,list_id,parent_task_id,title,description,status,priority,due_date,completed_at,estimated_cost,recurrence_rule,position) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    task.getUserId(),task.getListId(),task.getParentTaskId(),task.getTitle(),task.getDescription(),task.getStatus(),task.getPriority(),task.getDueDate(),task.getCompletedAt(),task.getEstimatedCost(),task.getRecurrenceRule(),task.getPosition()==null?0:task.getPosition());
            task.setId(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        } else {
            jdbc.update("UPDATE tasks SET user_id=?,list_id=?,parent_task_id=?,title=?,description=?,status=?,priority=?,due_date=?,completed_at=?,estimated_cost=?,recurrence_rule=?,position=? WHERE id=?",
                    task.getUserId(),task.getListId(),task.getParentTaskId(),task.getTitle(),task.getDescription(),task.getStatus(),task.getPriority(),task.getDueDate(),task.getCompletedAt(),task.getEstimatedCost(),task.getRecurrenceRule(),task.getPosition()==null?0:task.getPosition(),task.getId());
        }
        return task;
    }
    public boolean deleteById(Long id) { return jdbc.update("DELETE FROM tasks WHERE id=?", id) > 0; }
    public boolean existsByListId(Long id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM tasks WHERE list_id=?)", Boolean.class, id)); }
    public boolean existsByParentTaskId(Long id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM tasks WHERE parent_task_id=?)", Boolean.class, id)); }
}
