package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PackagePaymentReviewRequest(@NotBlank String token, @NotNull Boolean paid) {}
