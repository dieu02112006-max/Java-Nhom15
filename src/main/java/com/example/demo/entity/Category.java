package com.example.demo.entity;

import com.example.demo.enums.CategoryType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Category extends BaseEntity {

    // null = danh mục mặc định của hệ thống
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Danh mục cha (null = danh mục gốc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;
}