package com.example.demo.entity;

import com.example.demo.enums.WalletType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Wallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletType type;

    // Số dư hiện tại
    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;
}