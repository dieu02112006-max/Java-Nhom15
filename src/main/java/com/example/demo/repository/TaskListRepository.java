package com.example.demo.repository;

import com.example.demo.entity.TaskList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaskListRepository {
    private final JdbcTemplate jdbc;
    public TaskListRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<TaskList> findAll() {
        return jdbc.query("SELECT id,user_id,name,color,position FROM task_lists ORDER BY id",
                (rs, row) -> new TaskList(rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"), rs.getString("color"), rs.getInt("position")));
    }
    public Optional<TaskList> findById(Long id) { return findAll().stream().filter(item -> item.getId().equals(id)).findFirst(); }
    public TaskList save(TaskList item) {
        if (item.getId() == null) {
            jdbc.update("INSERT INTO task_lists (user_id,name,color,position) VALUES (?,?,?,?)", item.getUserId(), item.getName(), item.getColor(), item.getPosition()==null?0:item.getPosition());
            item.setId(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        } else jdbc.update("UPDATE task_lists SET user_id=?,name=?,color=?,position=? WHERE id=?", item.getUserId(), item.getName(), item.getColor(), item.getPosition()==null?0:item.getPosition(), item.getId());
        return item;
    }
    public boolean deleteById(Long id) { return jdbc.update("DELETE FROM task_lists WHERE id=?", id) > 0; }
}
