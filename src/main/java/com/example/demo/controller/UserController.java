package com.example.demo.controller;

import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.request.UserRoleRequest;
import com.example.demo.dto.request.UserStatusRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/role")
    public UserResponse changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleRequest req,
            @AuthenticationPrincipal AppUserPrincipal currentUser) {
        if (currentUser.id().equals(id)) {
            throw new BusinessException("Không thể tự thay đổi quyền của tài khoản đang đăng nhập");
        }
        return service.changeRole(id, req.role());
    }

    @PutMapping("/{id}/status")
    public UserResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest req,
            @AuthenticationPrincipal AppUserPrincipal currentUser) {
        return service.changeActiveStatus(id, req.active(), currentUser.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal currentUser) {
        if (currentUser.id().equals(id)) {
            throw new BusinessException("Không thể tự xóa tài khoản đang đăng nhập");
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
