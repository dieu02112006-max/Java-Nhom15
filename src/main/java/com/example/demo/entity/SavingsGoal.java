package com.example.demo.entity;

import com.example.demo.entity.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;
}