package com.example.demo.controller;

import com.example.demo.dto.request.RecurringTransactionRequest;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(@Valid @RequestBody RecurringTransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<RecurringTransactionResponse> getByUser(@RequestParam Long userId) {
        return service.getByUser(userId);
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(@PathVariable Long id,
                                               @Valid @RequestBody RecurringTransactionRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // chạy job ngay để test bằng Postman, khỏi chờ đến 01:00 sáng
    @PostMapping("/run-due")
    public ResponseEntity<Void> runDue() {
        service.generateDueTransactions();
        return ResponseEntity.noContent().build();
    }
}