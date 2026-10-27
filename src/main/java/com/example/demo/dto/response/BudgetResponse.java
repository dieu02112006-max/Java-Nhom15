package com.example.demo.dto.response;

import com.example.demo.entity.enums.BudgetPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long id, Long userId, Long categoryId, String categoryName,
        BigDecimal amount, BudgetPeriod period, LocalDate startDate, LocalDate endDate,
        Integer alertThreshold,
        BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed, boolean overThreshold
) {}