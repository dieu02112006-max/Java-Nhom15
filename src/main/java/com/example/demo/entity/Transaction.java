package com.example.demo.entity;

import com.example.demo.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id, transaction_date"),
        @Index(name = "idx_transactions_wallet", columnList = "wallet_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Transaction extends BaseEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Giao dịch chuyển ví (TRANSFER) có thể không có danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Ví nhận tiền, chỉ dùng khi type = TRANSFER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    // Ảnh hóa đơn
    @Column(name = "receipt_url")
    private String receiptUrl;

    // Liên kết với to-do
    // Optional reference to the To-do module; kept as an ID in this expense-only scope.
    @Column(name = "task_id")
    private Long taskId;

    // Sinh ra từ giao dịch định kỳ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_id")
    private RecurringTransaction recurring;
}
