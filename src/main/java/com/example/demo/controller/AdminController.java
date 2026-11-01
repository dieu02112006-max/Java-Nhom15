package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final JdbcTemplate jdbc;
    public AdminController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/overview")
    public Overview overview() {
        Totals totals = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM users) users,
                       (SELECT COUNT(*) FROM tasks) tasks,
                       (SELECT COUNT(*) FROM tasks WHERE status <> 'done') openTasks,
                       (SELECT COUNT(*) FROM task_lists) lists,
                       (SELECT COUNT(*) FROM transactions WHERE deleted_at IS NULL) transactions,
                       (SELECT COUNT(*) FROM wallets WHERE is_archived = 0) wallets,
                       (SELECT COALESCE(SUM(balance),0) FROM wallets WHERE is_archived = 0) balance
                """, (rs, row) -> new Totals(rs.getLong("users"), rs.getLong("tasks"), rs.getLong("openTasks"),
                rs.getLong("lists"), rs.getLong("transactions"), rs.getLong("wallets"), rs.getBigDecimal("balance")));
        List<RecentTransaction> recent = jdbc.query("""
                SELECT t.id, u.full_name, t.type, t.amount, t.transaction_date, COALESCE(t.note, '') note
                FROM transactions t JOIN users u ON u.id=t.user_id
                WHERE t.deleted_at IS NULL ORDER BY t.transaction_date DESC, t.id DESC LIMIT 8
                """, (rs, row) -> new RecentTransaction(rs.getLong("id"), rs.getString("full_name"),
                rs.getString("type"), rs.getBigDecimal("amount"), rs.getTimestamp("transaction_date").toLocalDateTime(), rs.getString("note")));
        return new Overview(totals, recent);
    }

    @GetMapping("/data/{table}")
    public List<Map<String, Object>> table(@org.springframework.web.bind.annotation.PathVariable String table) {
        String sql = switch (table) {
            case "transactions" -> "SELECT t.id,u.full_name AS user_name,t.type,t.amount,t.transaction_date,t.note,COALESCE(c.name,'—') AS category_name,w.name AS wallet_name FROM transactions t JOIN users u ON u.id=t.user_id LEFT JOIN categories c ON c.id=t.category_id JOIN wallets w ON w.id=t.wallet_id WHERE t.deleted_at IS NULL ORDER BY t.transaction_date DESC LIMIT 500";
            case "wallets" -> "SELECT w.id,u.full_name AS user_name,w.name,w.type,w.balance,w.currency,w.is_archived FROM wallets w JOIN users u ON u.id=w.user_id ORDER BY w.id DESC LIMIT 500";
            case "budgets" -> "SELECT b.id,u.full_name AS user_name,COALESCE(c.name,'Tổng ngân sách') AS category_name,b.amount,b.period,b.start_date,b.end_date FROM budgets b JOIN users u ON u.id=b.user_id LEFT JOIN categories c ON c.id=b.category_id ORDER BY b.id DESC LIMIT 500";
            case "goals" -> "SELECT g.id,u.full_name AS user_name,g.name,g.target_amount,g.current_amount,g.deadline,g.status FROM savings_goals g JOIN users u ON u.id=g.user_id ORDER BY g.id DESC LIMIT 500";
            case "categories" -> "SELECT c.id,COALESCE(u.full_name,'Hệ thống') AS owner,c.name,c.type,c.icon,c.color FROM categories c LEFT JOIN users u ON u.id=c.user_id ORDER BY c.id DESC LIMIT 500";
            default -> throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        };
        return jdbc.queryForList(sql);
    }

    public record Overview(Totals totals, List<RecentTransaction> recentTransactions) {}
    public record Totals(long users, long tasks, long openTasks, long lists, long transactions, long wallets, BigDecimal balance) {}
    public record RecentTransaction(long id, String userName, String type, BigDecimal amount, java.time.LocalDateTime date, String note) {}
}
