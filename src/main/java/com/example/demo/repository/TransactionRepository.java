package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    List<Transaction> findByWalletIdOrderByTransactionDateDesc(Long walletId);

    // danh mục đã được dùng trong giao dịch nào chưa
    boolean existsByCategoryId(Long categoryId);

    // tổng chi trong một kỳ, dùng cho BudgetService (categoryId = null: tính mọi danh mục)
    @Query("""
           select coalesce(sum(t.amount), 0) from Transaction t
           where t.user.id = :userId
             and t.type = com.example.demo.entity.enums.TransactionType.EXPENSE
             and (:categoryId is null or t.category.id = :categoryId)
             and t.transactionDate between :from and :to
           """)
    BigDecimal sumExpense(@Param("userId") Long userId,
                          @Param("categoryId") Long categoryId,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);
}