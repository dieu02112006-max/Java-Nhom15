package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Task extends BaseEntity {
    // To-do module is outside the current implementation scope.
}
