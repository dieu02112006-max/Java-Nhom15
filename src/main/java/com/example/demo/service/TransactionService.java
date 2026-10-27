package com.example.demo.service;

import com.example.demo.dto.request.TransactionRequest;
import com.example.demo.dto.response.TransactionResponse;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.RecurringFrequency;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.entity.enums.WalletType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final WalletRepository walletRepo;
    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;
    private final RecurringTransactionRepository recurringRepo;

    public TransactionResponse create(TransactionRequest req) {
        Transaction t = new Transaction();
        fill(t, req);
        applyBalance(t, 1);
        ensureNotNegative(t.getWallet(), t.getToWallet());
        return toResponse(transactionRepo.save(t));
    }

    public TransactionResponse update(Long id, TransactionRequest req) {
        Transaction t = find(id);
        if (!t.getUser().getId().equals(req.userId())) {
            throw new BusinessException("Không thể đổi chủ sở hữu của giao dịch");
        }
        Wallet oldWallet = t.getWallet();
        Wallet oldToWallet = t.getToWallet();

        applyBalance(t, -1);          // 1. hoàn tác tác động cũ lên ví
        fill(t, req);                 // 2. ghi dữ liệu mới
        applyBalance(t, 1);           // 3. áp dụng tác động mới

        ensureNotNegative(oldWallet, oldToWallet, t.getWallet(), t.getToWallet());
        return toResponse(transactionRepo.save(t));
    }

    public void delete(Long id) {
        Transaction t = find(id);
        applyBalance(t, -1);
        ensureNotNegative(t.getWallet(), t.getToWallet());
        transactionRepo.delete(t);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return toResponse(find(id));
    }

    // mặc định lấy từ đầu tháng hiện tại đến hôm nay
    @Transactional(readOnly = true)
    public List<TransactionResponse> getByUser(Long userId, LocalDate from, LocalDate to) {
        LocalDate fromDate = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return transactionRepo
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        userId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByWallet(Long walletId) {
        return transactionRepo.findByWalletIdOrderByTransactionDateDesc(walletId)
                .stream().map(this::toResponse).toList();
    }

    /** Được RecurringTransactionService gọi: sinh giao dịch thật cho một khoản định kỳ đã đến hạn. */
    public void generateFromRecurring(Long recurringId, LocalDate today) {
        RecurringTransaction r = recurringRepo.findById(recurringId)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", recurringId));

        while (r.isActive() && !r.getNextRunDate().isAfter(today)) {
            if (r.getEndDate() != null && r.getNextRunDate().isAfter(r.getEndDate())) {
                r.setActive(false);
                break;
            }
            Transaction t = Transaction.builder()
                    .user(r.getUser())
                    .wallet(r.getWallet())
                    .category(r.getCategory())
                    .type(r.getType())
                    .amount(r.getAmount())
                    .transactionDate(r.getNextRunDate().atStartOfDay())
                    .note(r.getNote())
                    .recurring(r)
                    .build();
            applyBalance(t, 1);
            ensureNotNegative(t.getWallet());
            transactionRepo.save(t);

            r.setNextRunDate(nextDate(r.getNextRunDate(), r.getFrequency()));
            if (r.getEndDate() != null && r.getNextRunDate().isAfter(r.getEndDate())) {
                r.setActive(false);
            }
        }
    }

    // ---------- helper ----------

    private void fill(Transaction t, TransactionRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        Wallet wallet = findOwnedWallet(req.walletId(), user.getId());
        Category category = null;
        Wallet toWallet = null;

        if (req.type() == TransactionType.TRANSFER) {
            if (req.toWalletId() == null) {
                throw new BusinessException("Chuyển ví cần có ví nhận (toWalletId)");
            }
            if (req.toWalletId().equals(req.walletId())) {
                throw new BusinessException("Ví nhận phải khác ví nguồn");
            }
            toWallet = findOwnedWallet(req.toWalletId(), user.getId());
        } else {
            if (req.categoryId() == null) {
                throw new BusinessException("Giao dịch thu/chi cần có danh mục (categoryId)");
            }
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
            if (!category.getType().name().equals(req.type().name())) {
                throw new BusinessException("Loại danh mục không khớp với loại giao dịch");
            }
            if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Danh mục không thuộc về người dùng này");
            }
        }

        t.setUser(user);
        t.setWallet(wallet);
        t.setCategory(category);
        t.setToWallet(toWallet);
        t.setType(req.type());
        t.setAmount(req.amount());
        t.setTransactionDate(req.transactionDate());
        t.setNote(req.note());
        t.setReceiptUrl(req.receiptUrl());
    }

    private Wallet findOwnedWallet(Long walletId, Long userId) {
        Wallet w = walletRepo.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", walletId));
        if (!w.getUser().getId().equals(userId)) {
            throw new BusinessException("Ví không thuộc về người dùng này");
        }
        if (w.isArchived()) {
            throw new BusinessException("Ví \"" + w.getName() + "\" đã được lưu trữ");
        }
        return w;
    }

    // sign = 1: áp dụng giao dịch, sign = -1: hoàn tác giao dịch
    private void applyBalance(Transaction t, int sign) {
        BigDecimal delta = t.getAmount().multiply(BigDecimal.valueOf(sign));
        Wallet w = t.getWallet();
        switch (t.getType()) {
            case INCOME -> w.setBalance(w.getBalance().add(delta));
            case EXPENSE -> w.setBalance(w.getBalance().subtract(delta));
            case TRANSFER -> {
                w.setBalance(w.getBalance().subtract(delta));
                Wallet to = t.getToWallet();
                to.setBalance(to.getBalance().add(delta));
            }
        }
    }

    // ví tín dụng được phép âm, các loại ví khác thì không
    private void ensureNotNegative(Wallet... wallets) {
        for (Wallet w : wallets) {
            if (w != null && w.getType() != WalletType.CREDIT && w.getBalance().signum() < 0) {
                throw new BusinessException("Ví \"" + w.getName() + "\" không đủ số dư");
            }
        }
    }

    private LocalDate nextDate(LocalDate d, RecurringFrequency f) {
        return switch (f) {
            case DAILY -> d.plusDays(1);
            case WEEKLY -> d.plusWeeks(1);
            case MONTHLY -> d.plusMonths(1);
            case YEARLY -> d.plusYears(1);
        };
    }

    private Transaction find(Long id) {
        return transactionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private TransactionResponse toResponse(Transaction t) {
        Category c = t.getCategory();
        Wallet to = t.getToWallet();
        return new TransactionResponse(
                t.getId(), t.getUser().getId(),
                t.getWallet().getId(), t.getWallet().getName(),
                c != null ? c.getId() : null, c != null ? c.getName() : null,
                t.getType(), t.getAmount(), t.getTransactionDate(), t.getNote(),
                to != null ? to.getId() : null, to != null ? to.getName() : null,
                t.getReceiptUrl(),
                t.getRecurring() != null ? t.getRecurring().getId() : null);
    }
}