package com.example.demo.dto.response;

import com.example.demo.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id, Long userId,
        Long walletId, String walletName,
        Long categoryId, String categoryName,
        TransactionType type, BigDecimal amount, LocalDateTime transactionDate,
        String note,
        Long toWalletId, String toWalletName,
        String receiptUrl, Long recurringId
) {}