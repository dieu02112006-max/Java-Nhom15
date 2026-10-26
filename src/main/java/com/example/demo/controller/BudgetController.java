package com.example.demo.controller;

import com.example.demo.dto.request.BudgetRequest;
import com.example.demo.dto.response.BudgetResponse;
import com.example.demo.service.BudgetService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest req) {
        authorization.requireUserAccess(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<BudgetResponse> getByUser(@RequestParam Long userId) {
        authorization.requireUserAccess(userId);
        return service.getByUser(userId);
    }

    @GetMapping("/{id}")
    public BudgetResponse getById(@PathVariable Long id) {
        BudgetResponse budget = service.getById(id);
        authorization.requireUserAccess(budget.userId());
        return budget;
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable Long id, @Valid @RequestBody BudgetRequest req) {
        authorization.requireUserAccess(service.getById(id).userId());
        authorization.requireUserAccess(req.userId());
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorization.requireUserAccess(service.getById(id).userId());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
