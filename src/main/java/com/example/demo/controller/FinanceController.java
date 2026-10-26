package com.example.demo.controller;

import com.example.demo.service.FinanceService;
import com.example.demo.service.FinanceService.CategoryOption;
import com.example.demo.service.FinanceService.FinanceSummary;
import com.example.demo.service.FinanceService.TransactionRow;
import com.example.demo.service.FinanceService.WalletOption;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    public FinanceSummary summary(
            @RequestParam(defaultValue = "1") long userId,
            @RequestParam(required = false) String month) {
        return service.summary(userId, parseMonth(month));
    }

    @GetMapping("/transactions")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    public List<TransactionRow> transactions(
            @RequestParam(defaultValue = "1") long userId,
            @RequestParam(required = false) String month) {
        return service.transactions(userId, parseMonth(month));
    }

    @GetMapping("/categories")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    public List<CategoryOption> categories(
            @RequestParam(defaultValue = "1") long userId,
            @RequestParam(defaultValue = "expense") String type) {
        return service.categories(userId, type);
    }

    @GetMapping("/wallets")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    public List<WalletOption> wallets(@RequestParam(defaultValue = "1") long userId) {
        return service.wallets(userId);
    }

    @PostMapping("/transactions")
    @PreAuthorize("@authz.canAccessUser(#transaction.userId)")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionRow create(@RequestBody FinanceService.NewTransaction transaction) {
        return service.create(transaction);
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("@authz.canAccessUser(#userId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable long id,
            @RequestParam(defaultValue = "1") long userId) {
        service.delete(id, userId);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "month must use YYYY-MM format", exception);
        }
    }
}
