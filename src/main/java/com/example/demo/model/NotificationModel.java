package com.example.demo.model;

import java.time.LocalDateTime;

public record NotificationModel(Long id, Long userId, String type, String title, String content, boolean isRead, LocalDateTime createdAt) { }
