package com.example.demo.service;

import com.example.demo.dto.response.PackageOverviewResponse;
import com.example.demo.dto.response.PackagePlanResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageService {
    private static final List<PackagePlanResponse> PLANS = List.of(
            new PackagePlanResponse("FREE", "Miễn phí", 0, 1,
                    List.of("Công việc, lịch, mục tiêu và thống kê cơ bản")),
            new PackagePlanResponse("PLUS", "Chủ đề", 100_000, 2,
                    List.of("Tất cả chức năng cơ bản", "Mở khóa chủ đề và bảng màu")),
            new PackagePlanResponse("PRO", "Toàn diện", 500_000, 3,
                    List.of("Tất cả chức năng", "Chủ đề và bảng màu", "Trợ lý AI Chibi")));

    private final JdbcTemplate jdbc;
    private final ManualPackagePaymentService paymentService;

    public PackageService(JdbcTemplate jdbc, ManualPackagePaymentService paymentService) {
        this.jdbc = jdbc;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public PackageOverviewResponse overview(Long userId) {
        return new PackageOverviewResponse(currentPlan(userId), PLANS, paymentService.isEnabled());
    }

    @Transactional(readOnly = true)
    public PackagePlanResponse currentPlan(Long userId) {
        List<String> codes = jdbc.queryForList(
                "SELECT subscription_tier FROM users WHERE id = ?", String.class, userId);
        if (codes.isEmpty()) throw new ResourceNotFoundException("User", userId);
        return planOrFree(codes.getFirst());
    }

    @Transactional
    public PackagePlanResponse purchase(Long userId, String planCode) {
        PackagePlanResponse selected = planByCode(planCode);
        if (selected.priceVnd() > 0) throw new BusinessException("Gói trả phí cần tạo yêu cầu thanh toán");
        int updated = jdbc.update("UPDATE users SET subscription_tier = ? WHERE id = ?", selected.code(), userId);
        if (updated == 0) throw new ResourceNotFoundException("User", userId);
        return selected;
    }

    public PackagePlanResponse planByCode(String planCode) {
        if (planCode == null) throw new BusinessException("Bạn cần chọn một gói");
        return PLANS.stream()
                .filter(plan -> plan.code().equalsIgnoreCase(planCode.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Gói bạn chọn không tồn tại"));
    }

    public void requireThemeAccess(Long userId) {
        if (currentPlan(userId).rank() < 2) {
            throw new BusinessException("Chức năng chủ đề dành cho gói Chủ đề hoặc Toàn diện");
        }
    }

    private PackagePlanResponse planOrFree(String code) {
        if (code == null) return PLANS.getFirst();
        Map<String, PackagePlanResponse> byCode = Map.of(
                "FREE", PLANS.get(0), "PLUS", PLANS.get(1), "PRO", PLANS.get(2));
        return byCode.getOrDefault(code.toUpperCase(), PLANS.getFirst());
    }
}
