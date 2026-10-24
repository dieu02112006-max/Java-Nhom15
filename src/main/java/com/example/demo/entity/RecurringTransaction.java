package com.example.demo.entity;

import com.example.demo.enums.Frequency;
import com.example.demo.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RecurringTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Chỉ dùng INCOME hoặc EXPENSE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    // null = lặp vô thời hạn
    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
