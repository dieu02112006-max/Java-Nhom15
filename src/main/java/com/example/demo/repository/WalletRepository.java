package com.example.demo.repository;

import com.example.demo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // ví chưa lưu trữ của một người dùng
    List<Wallet> findByUserIdAndArchivedFalse(Long userId);

    // kiểm tra trùng tên ví (không phân biệt hoa thường)
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}