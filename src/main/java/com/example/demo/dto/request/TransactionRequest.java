package com.example.demo.dto.request;

import com.example.demo.entity.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
        @NotNull Long userId,
        @NotNull Long walletId,
        Long categoryId,                             // bắt buộc với INCOME/EXPENSE, bỏ trống với TRANSFER
        @NotNull TransactionType type,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDateTime transactionDate,
        String note,
        Long toWalletId,                             // bắt buộc với TRANSFER
        String receiptUrl
) {}