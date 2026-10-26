package com.example.demo.dto.response;

import com.example.demo.entity.enums.WalletType;
import java.math.BigDecimal;

public record WalletResponse(
        Long id, Long userId, String name, WalletType type,
        BigDecimal balance, String currency, boolean archived
) {}