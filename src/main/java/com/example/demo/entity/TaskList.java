package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskList {
    private Long id;
    private Long userId;
    private String name;
    private String color;
    private Integer position;
}
