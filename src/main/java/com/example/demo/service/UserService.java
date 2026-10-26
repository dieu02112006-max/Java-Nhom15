package com.example.demo.service;

import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống");
        }
        if (userRepo.existsByEmail(req.email())) {
            throw new BusinessException("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .avatarUrl(req.avatarUrl())
                .timezone(req.timezone() == null ? "Asia/Ho_Chi_Minh" : req.timezone())
                .build();
        if (req.currencyDefault() != null) {
            user.setCurrencyDefault(req.currencyDefault());
        }
        return toResponse(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(find(id));
    }

    public UserResponse update(Long id, UserRequest req) {
        User user = find(id);
        if (!user.getEmail().equals(req.email()) && userRepo.existsByEmail(req.email())) {
            throw new BusinessException("Email đã được sử dụng");
        }
        user.setEmail(req.email());
        user.setFullName(req.fullName());
        user.setAvatarUrl(req.avatarUrl());
        if (req.timezone() != null) {
            user.setTimezone(req.timezone());
        }
        if (req.currencyDefault() != null) {
            user.setCurrencyDefault(req.currencyDefault());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return toResponse(userRepo.save(user));
    }

    public void delete(Long id) {
        userRepo.delete(find(id));
    }

    private User find(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.getCurrencyDefault(), user.getTimezone(), user.getCreatedAt());
    }
}
