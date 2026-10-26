package com.example.demo.dto.request;

import com.example.demo.entity.enums.RecurringFrequency;
import com.example.demo.entity.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(
        @NotNull Long userId,
        @NotNull Long walletId,
        @NotNull Long categoryId,
        @NotNull @Positive BigDecimal amount,
        @NotNull TransactionType type,               // chỉ INCOME hoặc EXPENSE
        String note,
        @NotNull RecurringFrequency frequency,
        @NotNull LocalDate nextRunDate,
        LocalDate endDate
) {}