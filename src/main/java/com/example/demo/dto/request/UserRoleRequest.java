package com.example.demo.dto.request;

import com.example.demo.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull UserRole role) {
}
