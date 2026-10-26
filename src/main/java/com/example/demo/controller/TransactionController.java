package com.example.demo.controller;

import com.example.demo.dto.request.TransactionRequest;
import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.service.TransactionService;
import com.example.demo.security.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;
    private final AuthorizationService authorization;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest req) {
        authorization.requireUserAccess(req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    // GET /api/transactions?userId=1&from=2026-10-01&to=2026-10-31
    @GetMapping
    public List<TransactionResponse> getByUser(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authorization.requireUserAccess(userId);
        return service.getByUser(userId, from, to);
    }

    @GetMapping("/wallet/{walletId}")
    public List<TransactionResponse> getByWallet(@PathVariable Long walletId) {
        List<TransactionResponse> rows = service.getByWallet(walletId);
        rows.forEach(row -> authorization.requireUserAccess(row.userId()));
        return rows;
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable Long id) {
        TransactionResponse transaction = service.getById(id);
        authorization.requireUserAccess(transaction.userId());
        return transaction;
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest req) {
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
