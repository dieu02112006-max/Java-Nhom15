package com.example.demo.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FinanceService {

    private final JdbcTemplate jdbcTemplate;

    public FinanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FinanceSummary summary(long userId, YearMonth month) {
        requireUser(userId);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        FinanceSummary totals = jdbcTemplate.queryForObject("""
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income,
                    COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) AS expense
                FROM transactions
                WHERE user_id = ? AND deleted_at IS NULL
                    AND transaction_date >= ? AND transaction_date < ?
                """, (rs, rowNum) -> new FinanceSummary(
                rs.getBigDecimal("income"),
                rs.getBigDecimal("expense"),
                BigDecimal.ZERO,
                BigDecimal.ZERO), userId, Timestamp.valueOf(start), Timestamp.valueOf(end));
        BigDecimal budget = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                    (SELECT SUM(amount) FROM budgets
                     WHERE user_id = ? AND category_id IS NULL
                        AND start_date < ? AND end_date >= ?),
                    (SELECT SUM(amount) FROM budgets
                     WHERE user_id = ? AND category_id IS NOT NULL
                        AND start_date < ? AND end_date >= ?),
                    0
                )
                """, BigDecimal.class, userId, end.toLocalDate(), start.toLocalDate(),
                userId, end.toLocalDate(), start.toLocalDate());
        BigDecimal balance = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(balance), 0)
                FROM wallets
                WHERE user_id = ? AND is_archived = 0
                """, BigDecimal.class, userId);
        return new FinanceSummary(
                totals.income(),
                totals.expense(),
                budget,
                balance);
    }

    public List<TransactionRow> transactions(long userId, YearMonth month) {
        requireUser(userId);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        return jdbcTemplate.query("""
                SELECT t.id, t.type, t.amount, t.transaction_date, t.note,
                    t.category_id, t.wallet_id,
                    COALESCE(c.name, 'Chưa phân loại') AS category_name,
                    w.name AS wallet_name
                FROM transactions t
                LEFT JOIN categories c ON c.id = t.category_id
                JOIN wallets w ON w.id = t.wallet_id
                WHERE t.user_id = ? AND t.deleted_at IS NULL
                    AND t.transaction_date >= ? AND t.transaction_date < ?
                    AND t.type IN ('income', 'expense')
                ORDER BY t.transaction_date DESC, t.id DESC
                """, (rs, rowNum) -> new TransactionRow(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("transaction_date").toLocalDateTime(),
                rs.getString("note"),
                rs.getLong("category_id") == 0 ? null : rs.getLong("category_id"),
                rs.getLong("wallet_id"),
                rs.getString("category_name"),
                rs.getString("wallet_name")), userId, Timestamp.valueOf(start), Timestamp.valueOf(end));
    }

    public List<CategoryOption> categories(long userId, String type) {
        requireUser(userId);
        if (!"income".equals(type) && !"expense".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be income or expense");
        }
        return jdbcTemplate.query("""
                SELECT id, name, color
                FROM categories
                WHERE type = ? AND (user_id IS NULL OR user_id = ?)
                ORDER BY user_id IS NOT NULL, name
                """, (rs, rowNum) -> new CategoryOption(
                rs.getLong("id"), rs.getString("name"), rs.getString("color")), type, userId);
    }

    public List<WalletOption> wallets(long userId) {
        requireUser(userId);
        return jdbcTemplate.query("""
                SELECT id, name, balance
                FROM wallets
                WHERE user_id = ? AND is_archived = 0
                ORDER BY name
                """, (rs, rowNum) -> new WalletOption(
                rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("balance")), userId);
    }

    @Transactional
    public TransactionRow create(NewTransaction transaction) {
        if (transaction == null || transaction.userId() == null || transaction.walletId() == null
                || transaction.amount() == null || transaction.transactionDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "userId, walletId, amount and transactionDate are required");
        }
        if (transaction.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than zero");
        }
        if (!"income".equals(transaction.type()) && !"expense".equals(transaction.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be income or expense");
        }
        long userId = transaction.userId();
        requireUser(userId);

        int walletExists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM wallets
                WHERE id = ? AND user_id = ? AND is_archived = 0
                """, Integer.class, transaction.walletId(), userId);
        if (walletExists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet not found for this user");
        }
        if (transaction.categoryId() != null) {
            int categoryExists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM categories
                    WHERE id = ? AND type = ? AND (user_id IS NULL OR user_id = ?)
                    """, Integer.class, transaction.categoryId(), transaction.type(), userId);
            if (categoryExists == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is invalid for this transaction");
            }
        }

        String note = transaction.note() == null || transaction.note().isBlank()
                ? null : transaction.note().trim();
        jdbcTemplate.update("""
                INSERT INTO transactions (user_id, wallet_id, category_id, type, amount, transaction_date, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, userId, transaction.walletId(), transaction.categoryId(), transaction.type(),
                transaction.amount(), Timestamp.valueOf(transaction.transactionDate()), note);
        jdbcTemplate.update("""
                UPDATE wallets SET balance = balance + ?
                WHERE id = ? AND user_id = ?
                """, walletDelta(transaction.type(), transaction.amount()), transaction.walletId(), userId);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return findTransaction(id, userId);
    }

    @Transactional
    public TransactionRow update(long id, long userId, NewTransaction transaction) {
        if (transaction == null || transaction.walletId() == null
                || transaction.amount() == null || transaction.transactionDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "walletId, amount and transactionDate are required");
        }
        if (transaction.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than zero");
        }
        if (!"income".equals(transaction.type()) && !"expense".equals(transaction.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be income or expense");
        }
        requireUser(userId);
        List<DeleteInfo> rows = jdbcTemplate.query("""
                SELECT wallet_id, type, amount
                FROM transactions
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                FOR UPDATE
                """, (rs, rowNum) -> new DeleteInfo(
                rs.getLong("wallet_id"), rs.getString("type"), rs.getBigDecimal("amount")), id, userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        int walletExists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM wallets WHERE id = ? AND user_id = ? AND is_archived = 0
                """, Integer.class, transaction.walletId(), userId);
        if (walletExists == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet not found for this user");
        if (transaction.categoryId() != null) {
            int categoryExists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM categories
                    WHERE id = ? AND type = ? AND (user_id IS NULL OR user_id = ?)
                    """, Integer.class, transaction.categoryId(), transaction.type(), userId);
            if (categoryExists == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is invalid for this transaction");
        }
        DeleteInfo old = rows.get(0);
        jdbcTemplate.update("""
                UPDATE wallets SET balance = balance - ? WHERE id = ? AND user_id = ?
                """, walletDelta(old.type(), old.amount()), old.walletId(), userId);
        jdbcTemplate.update("""
                UPDATE transactions SET wallet_id = ?, category_id = ?, type = ?, amount = ?, transaction_date = ?, note = ?
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """, transaction.walletId(), transaction.categoryId(), transaction.type(), transaction.amount(),
                Timestamp.valueOf(transaction.transactionDate()),
                transaction.note() == null || transaction.note().isBlank() ? null : transaction.note().trim(), id, userId);
        jdbcTemplate.update("""
                UPDATE wallets SET balance = balance + ? WHERE id = ? AND user_id = ?
                """, walletDelta(transaction.type(), transaction.amount()), transaction.walletId(), userId);
        return findTransaction(id, userId);
    }

    @Transactional
    public void delete(long id, long userId) {
        requireUser(userId);
        List<DeleteInfo> rows = jdbcTemplate.query("""
                SELECT wallet_id, type, amount
                FROM transactions
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                FOR UPDATE
                """, (rs, rowNum) -> new DeleteInfo(
                rs.getLong("wallet_id"),
                rs.getString("type"),
                rs.getBigDecimal("amount")), id, userId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        }
        DeleteInfo transaction = rows.get(0);
        jdbcTemplate.update("""
                UPDATE transactions SET deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """, id, userId);
        jdbcTemplate.update("""
                UPDATE wallets SET balance = balance - ?
                WHERE id = ? AND user_id = ?
                """, walletDelta(transaction.type(), transaction.amount()),
                transaction.walletId(), userId);
    }

    private TransactionRow findTransaction(long id, long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT t.id, t.type, t.amount, t.transaction_date, t.note,
                    t.category_id, t.wallet_id,
                    COALESCE(c.name, 'Chưa phân loại') AS category_name,
                    w.name AS wallet_name
                FROM transactions t
                LEFT JOIN categories c ON c.id = t.category_id
                JOIN wallets w ON w.id = t.wallet_id
                WHERE t.id = ? AND t.user_id = ?
                """, (rs, rowNum) -> new TransactionRow(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("transaction_date").toLocalDateTime(),
                rs.getString("note"),
                rs.getLong("category_id") == 0 ? null : rs.getLong("category_id"),
                rs.getLong("wallet_id"),
                rs.getString("category_name"),
                rs.getString("wallet_name")), id, userId);
    }

    private void requireUser(long userId) {
        if (userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be greater than zero");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private BigDecimal walletDelta(String type, BigDecimal amount) {
        return "income".equals(type) ? amount : amount.negate();
    }

    public record NewTransaction(
            Long userId,
            Long walletId,
            Long categoryId,
            String type,
            BigDecimal amount,
            LocalDateTime transactionDate,
            String note) {
    }

    public record TransactionRow(
            long id,
            String type,
            BigDecimal amount,
            LocalDateTime transactionDate,
            String note,
            Long categoryId,
            long walletId,
            String categoryName,
            String walletName) {
    }

    public record FinanceSummary(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal budget,
            BigDecimal walletBalance) {
    }

    public record CategoryOption(long id, String name, String color) {
    }

    public record WalletOption(long id, String name, BigDecimal balance) {
    }

    private record DeleteInfo(long walletId, String type, BigDecimal amount) {
    }
}
