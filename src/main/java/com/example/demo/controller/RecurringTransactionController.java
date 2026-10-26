package com.example.demo.controller;

import com.example.demo.dto.request.RecurringTransactionRequest;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.service.RecurringTransactionService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(@Valid @RequestBody RecurringTransactionRequest req) {
        authorization.requireUserAccess(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<RecurringTransactionResponse> getByUser(@RequestParam Long userId) {
        authorization.requireUserAccess(userId);
        return service.getByUser(userId);
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse getById(@PathVariable Long id) {
        RecurringTransactionResponse transaction = service.getById(id);
        authorization.requireUserAccess(transaction.userId());
        return transaction;
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(@PathVariable Long id,
                                               @Valid @RequestBody RecurringTransactionRequest req) {
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

    // chạy job ngay để test bằng Postman, khỏi chờ đến 01:00 sáng
    @PostMapping("/run-due")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> runDue() {
        service.generateDueTransactions();
        return ResponseEntity.noContent().build();
    }
}
