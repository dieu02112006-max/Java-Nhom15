package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @lombok.Builder.Default
    @Column(name = "currency_default", nullable = false, length = 3)
    private String currencyDefault = "VND";

    @lombok.Builder.Default
    @Column(nullable = false, length = 60)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "skin_id")
    private Long skinId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
