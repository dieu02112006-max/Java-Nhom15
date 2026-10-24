package com.example.demo.controller;

import com.example.demo.dto.request.DepositRequest;
import com.example.demo.dto.request.SavingsGoalRequest;
import com.example.demo.dto.response.SavingsGoalResponse;
import com.example.demo.service.SavingsGoalService;
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

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<SavingsGoalResponse> getByUser(@RequestParam Long userId) {
        return service.getByUser(userId);
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(@PathVariable Long id, @Valid @RequestBody SavingsGoalRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public SavingsGoalResponse deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest req) {
        return service.deposit(id, req);
    }
}