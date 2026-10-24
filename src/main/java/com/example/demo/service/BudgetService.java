package com.example.demo.service;

import com.example.demo.dto.request.BudgetRequest;
import com.example.demo.dto.response.BudgetResponse;
import com.example.demo.entity.Budget;
import com.example.demo.entity.Category;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.BudgetPeriod;
import com.example.demo.entity.enums.CategoryType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.BudgetRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepo;
    private final TransactionRepository transactionRepo;
    private final CategoryRepository categoryRepo;
    private final UserRepository userRepo;

    public BudgetResponse create(BudgetRequest req) {
        Budget b = new Budget();
        fill(b, req);
        return toResponse(budgetRepo.save(b));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getByUser(Long userId) {
        return budgetRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getById(Long id) {
        return toResponse(find(id));
    }

    public BudgetResponse update(Long id, BudgetRequest req) {
        Budget b = find(id);
        if (!b.getUser().getId().equals(req.userId())) {
            throw new BusinessException("Không thể đổi chủ sở hữu của ngân sách");
        }
        fill(b, req);
        return toResponse(budgetRepo.save(b));
    }

    public void delete(Long id) {
        budgetRepo.delete(find(id));
    }

    private void fill(Budget b, BudgetRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
            if (category.getType() != CategoryType.EXPENSE) {
                throw new BusinessException("Ngân sách chỉ áp dụng cho danh mục chi");
            }
        }
        LocalDate end = req.endDate() != null ? req.endDate() : defaultEnd(req.startDate(), req.period());
        if (end.isBefore(req.startDate())) {
            throw new BusinessException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        b.setUser(user);
        b.setCategory(category);
        b.setAmount(req.amount());
        b.setPeriod(req.period());
        b.setStartDate(req.startDate());
        b.setEndDate(end);
        if (req.alertThreshold() != null) {
            b.setAlertThreshold(req.alertThreshold());
        }
    }

    private LocalDate defaultEnd(LocalDate start, BudgetPeriod period) {
        return switch (period) {
            case WEEKLY -> start.plusWeeks(1).minusDays(1);
            case MONTHLY -> start.plusMonths(1).minusDays(1);
            case YEARLY -> start.plusYears(1).minusDays(1);
        };
    }

    private Budget find(Long id) {
        return budgetRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    // số đã chi KHÔNG lưu trong DB, luôn tính trực tiếp từ bảng transactions
    private BudgetResponse toResponse(Budget b) {
        Long categoryId = b.getCategory() != null ? b.getCategory().getId() : null;
        BigDecimal spent = transactionRepo.sumExpense(
                b.getUser().getId(), categoryId,
                b.getStartDate().atStartOfDay(), b.getEndDate().atTime(23, 59, 59));
        BigDecimal remaining = b.getAmount().subtract(spent);
        BigDecimal percent = spent.multiply(BigDecimal.valueOf(100))
                .divide(b.getAmount(), 2, RoundingMode.HALF_UP);
        boolean over = percent.compareTo(BigDecimal.valueOf(b.getAlertThreshold())) >= 0;

        return new BudgetResponse(b.getId(), b.getUser().getId(), categoryId,
                b.getCategory() != null ? b.getCategory().getName() : null,
                b.getAmount(), b.getPeriod(), b.getStartDate(), b.getEndDate(), b.getAlertThreshold(),
                spent, remaining, percent, over);
    }
}