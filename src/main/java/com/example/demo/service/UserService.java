package com.example.demo.service;

import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.Skin;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.SkinRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final SkinRepository skinRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public UserResponse create(UserRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống");
        }
        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(email)
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
        String email = req.email().trim().toLowerCase();
        if (!user.getEmail().equals(email) && userRepo.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }
        user.setEmail(email);
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
        User user = find(id);
        if (user.getRole() == UserRole.ADMIN && userRepo.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Không thể xóa quản trị viên cuối cùng");
        }
        userRepo.delete(user);
    }

    public UserResponse changeRole(Long id, UserRole role) {
        User user = find(id);
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
                && userRepo.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Hệ thống phải luôn có ít nhất một quản trị viên");
        }
        user.setRole(role);
        return toResponse(userRepo.save(user));
    }

    public UserResponse changeActiveStatus(Long id, boolean active, Long currentUserId) {
        User user = find(id);
        boolean currentActive = isActive(id);
        if (!active && currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException("Không thể tự vô hiệu hóa tài khoản đang đăng nhập");
        }
        if (!active && currentActive && user.getRole() == UserRole.ADMIN
                && jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND is_active = 1", Integer.class) <= 1) {
            throw new BusinessException("Không thể vô hiệu hóa quản trị viên cuối cùng đang hoạt động");
        }
        jdbc.update("UPDATE users SET is_active = ? WHERE id = ?", active, id);
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Skin getSkin(Long userId) {
        User user = find(userId);
        Skin selected = user.getSkinId() == null ? null : skinRepo.findById(user.getSkinId()).orElse(null);
        if (selected != null && selected.isActive()) return selected;
        return skinRepo.findAll().stream()
                .filter(Skin::isActive)
                .filter(Skin::isDefault)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Default skin", 0L));
    }

    public Skin selectSkin(Long userId, Long skinId) {
        Skin skin = skinRepo.findById(skinId)
                .orElseThrow(() -> new ResourceNotFoundException("Skin", skinId));
        if (!skin.isActive()) throw new BusinessException("Chủ đề này hiện không khả dụng");
        User user = find(userId);
        user.setSkinId(skin.getId());
        userRepo.save(user);
        return skin;
    }

    private User find(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.getCurrencyDefault(), user.getTimezone(), user.getCreatedAt(), user.getRole().name(), isActive(user.getId()));
    }

    private boolean isActive(Long userId) {
        Boolean active = jdbc.queryForObject("SELECT is_active = 1 FROM users WHERE id = ?", Boolean.class, userId);
        return Boolean.TRUE.equals(active);
    }
}
