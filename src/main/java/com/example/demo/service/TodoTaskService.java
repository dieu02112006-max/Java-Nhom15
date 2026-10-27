package com.example.demo.service;

import com.example.demo.dto.request.TodoTaskRequest;
import com.example.demo.dto.response.TodoTaskListResponse;
import com.example.demo.dto.response.TodoTaskResponse;
import com.example.demo.dto.request.TodoWorkGoalRequest;
import com.example.demo.dto.response.TodoWorkGoalResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TodoTaskService {

    private final JdbcTemplate jdbc;

    public TodoTaskService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<TodoTaskResponse> findAll(Long userId) {
        return jdbc.query("""
                SELECT id, user_id, list_id, parent_task_id, goal_id, title, description, category, location,
                    status, priority, due_date, completed_at, estimated_cost, recurrence_rule,
                    position, created_at, updated_at
                FROM tasks WHERE user_id = ?
                ORDER BY due_date IS NULL, due_date, position, id
                """, (rs, rowNum) -> mapTask(rs), userId);
    }

    @Transactional(readOnly = true)
    public List<TodoTaskListResponse> findLists(Long userId) {
        return jdbc.query("""
                SELECT id, name, color, position FROM task_lists
                WHERE user_id = ? ORDER BY position, id
                """, (rs, rowNum) -> new TodoTaskListResponse(
                rs.getLong("id"), rs.getString("name"), rs.getString("color"), rs.getInt("position")), userId);
    }

    @Transactional(readOnly = true)
    public List<TodoWorkGoalResponse> findGoals(Long userId) {
        return jdbc.query("""
                SELECT id, title, created_at FROM todo_work_goals
                WHERE user_id = ? ORDER BY created_at, id
                """, (rs, rowNum) -> new TodoWorkGoalResponse(rs.getLong("id"), rs.getString("title"),
                rs.getDate("created_at").toLocalDate()), userId);
    }

    public TodoWorkGoalResponse createGoal(Long userId, TodoWorkGoalRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO todo_work_goals (user_id, title, created_at)
                    VALUES (?, ?, ?)
                    """, new String[] {"id"});
            statement.setLong(1, userId);
            statement.setString(2, request.title().trim());
            statement.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            return statement;
        }, keyHolder);
        return findGoal(userId, keyHolder.getKey().longValue());
    }

    public void deleteGoal(Long userId, Long id) {
        jdbc.update("UPDATE tasks SET goal_id = NULL WHERE goal_id = ? AND user_id = ?", id, userId);
        if (jdbc.update("DELETE FROM todo_work_goals WHERE id = ? AND user_id = ?", id, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu công việc");
        }
    }

    private TodoWorkGoalResponse findGoal(Long userId, Long id) {
        return jdbc.query("""
                SELECT id, title, created_at FROM todo_work_goals WHERE id = ? AND user_id = ?
                """, rs -> {
            if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu công việc");
            return new TodoWorkGoalResponse(rs.getLong("id"), rs.getString("title"), rs.getDate("created_at").toLocalDate());
        }, id, userId);
    }

    public TodoTaskResponse create(Long userId, TodoTaskRequest request) {
        String status = normalizeStatus(request.status());
        String priority = normalizePriority(request.priority());
        Long listId = request.listId() == null ? findOrCreateList(userId) : requireList(userId, request.listId());
        requireGoal(userId, request.goalId());
        requireParent(userId, listId, request.parentTaskId(), null);
        validateCost(request.estimatedCost());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO tasks (user_id, list_id, parent_task_id, goal_id, title, description, category, location,
                        status, priority, due_date, completed_at, estimated_cost, recurrence_rule, position)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id"});
            statement.setLong(1, userId);
            statement.setLong(2, listId);
            if (request.parentTaskId() == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, request.parentTaskId());
            if (request.goalId() == null) statement.setNull(4, java.sql.Types.BIGINT); else statement.setLong(4, request.goalId());
            statement.setString(5, request.title().trim());
            statement.setString(6, normalizeText(request.description()));
            statement.setString(7, normalizeCategory(request.category()));
            statement.setString(8, normalizeText(request.location()));
            statement.setString(9, status);
            statement.setString(10, priority);
            statement.setTimestamp(11, toTimestamp(request.dueDate()));
            statement.setTimestamp(12, toTimestamp(resolveCompletedAt(status, request.completedAt())));
            if (request.estimatedCost() == null) statement.setObject(13, null); else statement.setBigDecimal(13, request.estimatedCost());
            statement.setString(14, normalizeText(request.recurrenceRule()));
            statement.setInt(15, request.position() == null ? 0 : request.position());
            return statement;
        }, keyHolder);
        return findById(userId, keyHolder.getKey().longValue());
    }

    public TodoTaskResponse update(Long userId, Long id, TodoTaskRequest request) {
        String status = normalizeStatus(request.status());
        String priority = normalizePriority(request.priority());
        Long listId = request.listId() == null ? requireExistingListId(userId, id) : requireList(userId, request.listId());
        requireGoal(userId, request.goalId());
        requireParent(userId, listId, request.parentTaskId(), id);
        validateCost(request.estimatedCost());
        int updated = jdbc.update("""
                UPDATE tasks SET list_id = ?, parent_task_id = ?, goal_id = ?, title = ?, description = ?, category = ?, location = ?,
                    status = ?, priority = ?, due_date = ?, completed_at = ?, estimated_cost = ?, recurrence_rule = ?,
                    position = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, listId, request.parentTaskId(), request.goalId(), request.title().trim(), normalizeText(request.description()),
                normalizeCategory(request.category()), normalizeText(request.location()), status, priority,
                toTimestamp(request.dueDate()), toTimestamp(resolveCompletedAt(status, request.completedAt())),
                request.estimatedCost(), normalizeText(request.recurrenceRule()), request.position() == null ? 0 : request.position(), id, userId);
        if (updated == 0) throw notFound();
        return findById(userId, id);
    }

    public void delete(Long userId, Long id) {
        if (jdbc.update("DELETE FROM tasks WHERE id = ? AND user_id = ?", id, userId) == 0) {
            throw notFound();
        }
    }

    private TodoTaskResponse findById(Long userId, Long id) {
        return jdbc.query("""
                SELECT id, user_id, list_id, parent_task_id, goal_id, title, description, category, location,
                    status, priority, due_date, completed_at, estimated_cost, recurrence_rule,
                    position, created_at, updated_at
                FROM tasks WHERE id = ? AND user_id = ?
                """, rs -> {
            if (!rs.next()) throw notFound();
            return mapTask(rs);
        }, id, userId);
    }

    private TodoTaskResponse mapTask(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new TodoTaskResponse(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("list_id"),
                rs.getObject("parent_task_id", Long.class), rs.getObject("goal_id", Long.class), rs.getString("title"), rs.getString("description"),
                rs.getString("category"), rs.getString("location"), rs.getString("status"), rs.getString("priority"),
                localDateTime(rs.getTimestamp("due_date")), localDateTime(rs.getTimestamp("completed_at")),
                rs.getBigDecimal("estimated_cost"), rs.getString("recurrence_rule"), rs.getInt("position"),
                localDateTime(rs.getTimestamp("created_at")), localDateTime(rs.getTimestamp("updated_at")));
    }

    private Long requireList(Long userId, Long listId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM task_lists WHERE id = ? AND user_id = ?",
                Integer.class, listId, userId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách công việc không hợp lệ");
        }
        return listId;
    }

    private void requireGoal(Long userId, Long goalId) {
        if (goalId == null) return;
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM todo_work_goals WHERE id = ? AND user_id = ?",
                Integer.class, goalId, userId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mục tiêu không hợp lệ");
        }
    }

    private Long requireExistingListId(Long userId, Long taskId) {
        List<Long> listIds = jdbc.query("SELECT list_id FROM tasks WHERE id = ? AND user_id = ?",
                (rs, rowNum) -> rs.getLong(1), taskId, userId);
        if (listIds.isEmpty()) throw notFound();
        return listIds.getFirst();
    }

    private void requireParent(Long userId, Long listId, Long parentTaskId, Long updatingId) {
        if (parentTaskId == null) return;
        if (parentTaskId.equals(updatingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Công việc không thể là công việc cha của chính nó");
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM tasks WHERE id = ? AND user_id = ? AND list_id = ?
                """, Integer.class, parentTaskId, userId, listId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Công việc cha phải thuộc cùng tài khoản và danh sách");
        }
    }

    private String normalizeStatus(String status) {
        String value = status == null || status.isBlank() ? "todo" : status;
        if (!List.of("todo", "in_progress", "done").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái công việc không hợp lệ");
        }
        return value;
    }

    private String normalizePriority(String priority) {
        String value = priority == null || priority.isBlank() ? "medium" : priority;
        if (!List.of("low", "medium", "high").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức ưu tiên không hợp lệ");
        }
        return value;
    }

    private String normalizeCategory(String category) {
        return category == null || category.isBlank() ? "Cá nhân" : category.trim();
    }

    private void validateCost(BigDecimal estimatedCost) {
        if (estimatedCost != null && estimatedCost.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi phí ước tính không được âm");
        }
    }

    private LocalDateTime resolveCompletedAt(String status, LocalDateTime completedAt) {
        return "done".equals(status) && completedAt == null ? LocalDateTime.now() : completedAt;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long findOrCreateList(Long userId) {
        List<Long> lists = jdbc.query("SELECT id FROM task_lists WHERE user_id = ? ORDER BY position, id LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), userId);
        if (!lists.isEmpty()) return lists.getFirst();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO task_lists (user_id, name, color, position) VALUES (?, 'Công việc', '#4568DF', 1)",
                    new String[] {"id"});
            statement.setLong(1, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc");
    }
}
