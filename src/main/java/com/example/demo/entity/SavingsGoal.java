package com.example.demo.entity;

import com.example.demo.enums.SavingsGoalStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SavingsGoal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Builder.Default
    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDate deadline;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SavingsGoalStatus status = SavingsGoalStatus.ACTIVE;
}