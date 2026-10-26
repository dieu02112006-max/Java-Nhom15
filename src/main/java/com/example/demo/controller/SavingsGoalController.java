package com.example.demo.controller;

import com.example.demo.dto.request.DepositRequest;
import com.example.demo.dto.request.SavingsGoalRequest;
import com.example.demo.dto.response.SavingsGoalResponse;
import com.example.demo.service.SavingsGoalService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest req) {
        authorization.requireUserAccess(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<SavingsGoalResponse> getByUser(@RequestParam Long userId) {
        authorization.requireUserAccess(userId);
        return service.getByUser(userId);
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse getById(@PathVariable Long id) {
        SavingsGoalResponse goal = service.getById(id);
        authorization.requireUserAccess(goal.userId());
        return goal;
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(@PathVariable Long id, @Valid @RequestBody SavingsGoalRequest req) {
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

    @PostMapping("/{id}/deposit")
    public SavingsGoalResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest req) {
        authorization.requireUserAccess(service.getById(id).userId());
        return service.deposit(id, req);
    }
}
