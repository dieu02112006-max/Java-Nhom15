package com.example.demo.model;

import java.time.LocalDateTime;

public record SkinModel(Long id, String name, String code, String primaryColor, String secondaryColor, String backgroundColor, String textColor, String previewUrl, boolean isDefault, boolean isActive, LocalDateTime createdAt) { }
