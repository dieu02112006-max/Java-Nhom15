package com.example.demo.repository;

import com.example.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // danh mục của người dùng + danh mục mặc định của hệ thống (user_id = null)
    @Query("select c from Category c where c.user.id = :userId or c.user is null")
    List<Category> findAvailableForUser(@Param("userId") Long userId);

    // danh mục này có danh mục con không
    boolean existsByParentId(Long parentId);
}