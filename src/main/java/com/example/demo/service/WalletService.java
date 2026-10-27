package com.example.demo.service;

import com.example.demo.dto.request.WalletRequest;
import com.example.demo.dto.response.WalletResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepo;
    private final UserRepository userRepo;

    public WalletResponse create(WalletRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        Wallet w = Wallet.builder()
                .user(user)
                .name(req.name())
                .type(req.type())
                .build();
        if (req.balance() != null) {
            w.setBalance(req.balance());
        }
        if (req.currency() != null) {
            w.setCurrency(req.currency());
        }
        return toResponse(walletRepo.save(w));
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getByUser(Long userId) {
        return walletRepo.findByUserIdAndArchivedFalse(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse getById(Long id) {
        return toResponse(find(id));
    }

    // không cho sửa balance: số dư chỉ thay đổi qua giao dịch
    public WalletResponse update(Long id, WalletRequest req) {
        Wallet w = find(id);
        w.setName(req.name());
        w.setType(req.type());
        if (req.currency() != null) {
            w.setCurrency(req.currency());
        }
        return toResponse(walletRepo.save(w));
    }

    // "xóa" ví = lưu trữ, vì ví đã có giao dịch thì không xóa cứng được
    public void archive(Long id) {
        Wallet w = find(id);
        w.setArchived(true);
        walletRepo.save(w);
    }

    private Wallet find(Long id) {
        return walletRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Wallet", id));
    }

    private WalletResponse toResponse(Wallet w) {
        return new WalletResponse(w.getId(), w.getUser().getId(), w.getName(), w.getType(),
                w.getBalance(), w.getCurrency(), w.isArchived());
    }
}