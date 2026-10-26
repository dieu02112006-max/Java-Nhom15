package com.example.demo.dto.request;

import com.example.demo.entity.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        Long userId,                                 // null = danh mục mặc định của hệ thống
        Long parentId,                               // null = danh mục gốc
        @NotBlank String name,
        @NotNull CategoryType type,
        String icon,
        String color
) {}