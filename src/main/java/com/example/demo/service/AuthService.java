package com.example.demo.service;

import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(UserRole.USER)
                .build();
        return userRepository.save(user);
    }
}
