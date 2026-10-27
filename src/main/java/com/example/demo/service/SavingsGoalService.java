package com.example.demo.service;

import com.example.demo.dto.request.DepositRequest;
import com.example.demo.dto.request.SavingsGoalRequest;
import com.example.demo.dto.response.SavingsGoalResponse;
import com.example.demo.entity.SavingsGoal;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.GoalStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.SavingsGoalRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepo;
    private final UserRepository userRepo;

    public SavingsGoalResponse create(SavingsGoalRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        SavingsGoal g = SavingsGoal.builder()
                .user(user)
                .name(req.name())
                .targetAmount(req.targetAmount())
                .deadline(req.deadline())
                .build();
        return toResponse(goalRepo.save(g));
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getByUser(Long userId) {
        return goalRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getById(Long id) {
        return toResponse(find(id));
    }

    public SavingsGoalResponse update(Long id, SavingsGoalRequest req) {
        SavingsGoal g = find(id);
        if (!g.getUser().getId().equals(req.userId())) {
            throw new BusinessException("Không thể đổi chủ sở hữu của mục tiêu tiết kiệm");
        }
        g.setName(req.name());
        g.setTargetAmount(req.targetAmount());
        g.setDeadline(req.deadline());
        return toResponse(goalRepo.save(g));
    }

    public void delete(Long id) {
        goalRepo.delete(find(id));
    }

    public SavingsGoalResponse deposit(Long id, DepositRequest req) {
        SavingsGoal g = find(id);
        if (g.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException("Chỉ nạp tiền được vào mục tiêu đang ở trạng thái ACTIVE");
        }
        g.setCurrentAmount(g.getCurrentAmount().add(req.amount()));
        if (g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0) {
            g.setStatus(GoalStatus.COMPLETED);
        }
        return toResponse(goalRepo.save(g));
    }

    private SavingsGoal find(Long id) {
        return goalRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("SavingsGoal", id));
    }

    private SavingsGoalResponse toResponse(SavingsGoal g) {
        BigDecimal percent = g.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return new SavingsGoalResponse(g.getId(), g.getUser().getId(), g.getName(),
                g.getTargetAmount(), g.getCurrentAmount(), percent, g.getDeadline(), g.getStatus());
    }
}