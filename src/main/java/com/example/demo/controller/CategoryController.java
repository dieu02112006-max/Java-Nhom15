package com.example.demo.controller;

import com.example.demo.dto.request.CategoryRequest;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.service.CategoryService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        authorization.requireOwnedOrAdmin(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    // danh mục của user + danh mục mặc định của hệ thống
    @GetMapping
    public List<CategoryResponse> getByUser(@RequestParam Long userId) {
        authorization.requireUserAccess(userId);
        return service.getAvailableForUser(userId);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        CategoryResponse category = service.getById(id);
        if (category.userId() != null) authorization.requireUserAccess(category.userId());
        return category;
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        authorization.requireOwnedOrAdmin(service.getById(id).userId());
        authorization.requireOwnedOrAdmin(req.userId());
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorization.requireOwnedOrAdmin(service.getById(id).userId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
