package com.example.demo.repository;

import com.example.demo.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findAllByUserIdOrderByNextRunDateAsc(Long userId);
}
