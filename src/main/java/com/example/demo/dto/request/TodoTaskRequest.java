package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TodoTaskRequest(
        Long listId,
        Long parentTaskId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 80) String category,
        @Size(max = 500) String location,
        String status,
        String priority,
        LocalDateTime dueDate,
        LocalDateTime completedAt,
        BigDecimal estimatedCost,
        @Size(max = 255) String recurrenceRule,
        Integer position) {
}
