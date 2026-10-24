package com.example.demo.repository;

import com.example.demo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findAllByUserId(Long userId);
}
