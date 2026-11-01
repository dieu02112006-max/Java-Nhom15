package com.example.demo.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id, String email, String fullName, String avatarUrl,
        String currencyDefault, String timezone, LocalDateTime createdAt, String role, boolean active
) {}
