package com.example.demo.dto.response;

public record AuthResponse(Long id, String email, String fullName, String role, String token) {
    public AuthResponse(Long id, String email, String fullName, String role) {
        this(id, email, fullName, role, null);
    }
}
