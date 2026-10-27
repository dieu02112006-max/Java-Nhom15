package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TodoWorkGoalRequest(
        @NotBlank String title
) {}
