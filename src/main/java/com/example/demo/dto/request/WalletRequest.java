package com.example.demo.dto.request;

import com.example.demo.entity.enums.WalletType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WalletRequest(
        @NotNull Long userId,
        @NotBlank String name,
        @NotNull WalletType type,
        @PositiveOrZero BigDecimal balance,          // số dư ban đầu, chỉ có tác dụng khi tạo ví
        @Size(min = 3, max = 3) String currency
) {}