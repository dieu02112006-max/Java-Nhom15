package com.example.demo.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {
    private Long id;
    private Long userId;
    private Long taskId;
    private Long budgetId;
    private LocalDateTime remindAt;
    private boolean sent;
}
