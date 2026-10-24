package com.example.demo.dto.response;

import com.example.demo.entity.enums.RecurringFrequency;
import com.example.demo.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(
        Long id, Long userId,
        Long walletId, String walletName,
        Long categoryId, String categoryName,
        BigDecimal amount, TransactionType type, String note,
        RecurringFrequency frequency, LocalDate nextRunDate, LocalDate endDate, boolean active
) {}