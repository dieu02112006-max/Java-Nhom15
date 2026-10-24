package com.example.demo.dto.response;

import com.example.demo.entity.enums.CategoryType;

public record CategoryResponse(
        Long id, Long userId, Long parentId, String name,
        CategoryType type, String icon, String color
) {}