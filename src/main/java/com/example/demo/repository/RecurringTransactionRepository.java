package com.example.demo.repository;

import com.example.demo.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(Long userId);

    boolean existsByCategoryId(Long categoryId);

    // dùng cho job @Scheduled: các khoản định kỳ đang bật và đã đến hạn
    List<RecurringTransaction> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}