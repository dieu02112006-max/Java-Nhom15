package com.example.demo.entity;

import com.example.demo.enums.BudgetPeriod;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // null = ngân sách tổng (không gắn danh mục nào)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Hạn mức
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BudgetPeriod period;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Phần trăm cảnh báo, ví dụ 80
    @Builder.Default
    @Column(name = "alert_threshold", nullable = false)
    private Integer alertThreshold = 80;
}