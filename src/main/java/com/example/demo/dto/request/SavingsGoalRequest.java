package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequest(
        @NotNull Long userId,
        @NotBlank String name,
        @NotNull @Positive BigDecimal targetAmount,
        LocalDate deadline
) {}