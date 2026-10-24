package com.example.demo.model;

import java.time.LocalDateTime;

public record AttachmentModel(Long id, Long userId, String entityType, Long entityId, String fileUrl, String fileName, LocalDateTime uploadedAt) { }
