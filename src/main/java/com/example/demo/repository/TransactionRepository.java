package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdAndDeletedAtIsNullOrderByTransactionDateDesc(Long userId);
    List<Transaction> findAllByUserIdAndTransactionDateBetweenAndDeletedAtIsNullOrderByTransactionDateDesc(Long userId, LocalDateTime from, LocalDateTime to);
}
