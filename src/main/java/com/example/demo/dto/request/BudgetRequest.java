package com.example.demo.dto.request;

import com.example.demo.entity.enums.BudgetPeriod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetRequest(
        @NotNull Long userId,
        Long categoryId,                             // null = ngân sách tổng
        @NotNull @Positive BigDecimal amount,
        @NotNull BudgetPeriod period,
        @NotNull LocalDate startDate,
        LocalDate endDate,                           // bỏ trống = tự tính theo period
        @Min(1) @Max(100) Integer alertThreshold
) {}