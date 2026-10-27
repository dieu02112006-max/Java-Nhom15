package com.example.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TodoTaskResponse(
        Long id,
        Long userId,
        Long listId,
        Long parentTaskId,
        String title,
        String description,
        String category,
        String location,
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt,
        BigDecimal estimatedCost,
        String recurrenceRule,
        Integer position,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
