package com.example.demo.dto.response;

import com.example.demo.entity.enums.GoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id, Long userId, String name,
        BigDecimal targetAmount, BigDecimal currentAmount, BigDecimal percent,
        LocalDate deadline, GoalStatus status
) {}