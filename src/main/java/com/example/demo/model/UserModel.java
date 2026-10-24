package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserModel(Long id, String email, @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String passwordHash, String fullName, String avatarUrl, String currencyDefault, String timezone, Long skinId, LocalDateTime createdAt, LocalDateTime updatedAt) { }
