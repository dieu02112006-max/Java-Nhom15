package com.example.demo.dto.response;

import java.time.LocalDate;

public record TodoWorkGoalResponse(Long id, String title, LocalDate createdAt) {}
