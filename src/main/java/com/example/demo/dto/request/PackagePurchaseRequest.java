package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PackagePurchaseRequest(@NotBlank String planCode) {}
