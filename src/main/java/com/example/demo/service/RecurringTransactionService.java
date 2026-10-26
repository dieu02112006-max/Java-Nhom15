package com.example.demo.service;

import com.example.demo.dto.request.RecurringTransactionRequest;
import com.example.demo.dto.response.RecurringTransactionResponse;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.TransactionType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionService transactionService;

    public RecurringTransactionResponse create(RecurringTransactionRequest req) {
        RecurringTransaction r = new RecurringTransaction();
        fill(r, req);
        return toResponse(recurringRepo.save(r));
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getByUser(Long userId) {
        return recurringRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse getById(Long id) {
        return toResponse(find(id));
    }

    public RecurringTransactionResponse update(Long id, RecurringTransactionRequest req) {
        RecurringTransaction r = find(id);
        if (!r.getUser().getId().equals(req.userId())) {
            throw new BusinessException("Không thể đổi chủ sở hữu của giao dịch định kỳ");
        }
        fill(r, req);
        return toResponse(recurringRepo.save(r));
    }

    public void delete(Long id) {
        recurringRepo.delete(find(id));
    }

    /**
     * Chạy lúc 01:00 mỗi ngày. Không mở transaction ở mức job: mỗi khoản định kỳ được xử lý
     * trong transaction riêng của TransactionService, nên một khoản lỗi không kéo theo các khoản khác.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateDueTransactions() {
        LocalDate today = LocalDate.now();
        List<Long> ids = recurringRepo.findByActiveTrueAndNextRunDateLessThanEqual(today)
                .stream().map(RecurringTransaction::getId).toList();
        for (Long id : ids) {
            try {
                transactionService.generateFromRecurring(id, today);
            } catch (Exception e) {
                log.warn("Không thể sinh giao dịch định kỳ id={}: {}", id, e.getMessage());
            }
        }
    }

    private void fill(RecurringTransaction r, RecurringTransactionRequest req) {
        if (req.type() == TransactionType.TRANSFER) {
            throw new BusinessException("Giao dịch định kỳ chỉ hỗ trợ thu hoặc chi");
        }
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        Wallet wallet = walletRepo.findById(req.walletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", req.walletId()));
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Ví không thuộc về người dùng này");
        }
        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        if (!category.getType().name().equals(req.type().name())) {
            throw new BusinessException("Loại danh mục không khớp với loại giao dịch");
        }
        if (req.endDate() != null && req.endDate().isBefore(req.nextRunDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày chạy kế tiếp");
        }

        r.setUser(user);
        r.setWallet(wallet);
        r.setCategory(category);
        r.setAmount(req.amount());
        r.setType(req.type());
        r.setNote(req.note());
        r.setFrequency(req.frequency());
        r.setNextRunDate(req.nextRunDate());
        r.setEndDate(req.endDate());
    }

    private RecurringTransaction find(Long id) {
        return recurringRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", id));
    }

    private RecurringTransactionResponse toResponse(RecurringTransaction r) {
        return new RecurringTransactionResponse(
                r.getId(), r.getUser().getId(),
                r.getWallet().getId(), r.getWallet().getName(),
                r.getCategory().getId(), r.getCategory().getName(),
                r.getAmount(), r.getType(), r.getNote(),
                r.getFrequency(), r.getNextRunDate(), r.getEndDate(), r.isActive());
    }
}