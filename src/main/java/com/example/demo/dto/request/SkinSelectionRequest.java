package com.example.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SkinSelectionRequest(@NotNull @Positive Long skinId) {}
