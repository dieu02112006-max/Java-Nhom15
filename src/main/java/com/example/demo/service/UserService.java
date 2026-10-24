package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.model.UserModel;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<UserModel> findAll() { return repository.findAll().stream().map(this::toModel).toList(); }
    @Transactional(readOnly = true) public UserModel findById(Long id) { return toModel(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id))); }
    public UserModel create(UserModel m) { User e = new User(); e.setEmail(m.email()); e.setPasswordHash(m.passwordHash()); e.setFullName(m.fullName()); e.setAvatarUrl(m.avatarUrl()); e.setCurrencyDefault(m.currencyDefault() == null ? "VND" : m.currencyDefault()); e.setTimezone(m.timezone() == null ? "Asia/Ho_Chi_Minh" : m.timezone()); e.setSkinId(m.skinId()); return toModel(repository.save(e)); }
    public UserModel update(Long id, UserModel m) { User e = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); e.setEmail(m.email()); if (m.passwordHash() != null) e.setPasswordHash(m.passwordHash()); e.setFullName(m.fullName()); e.setAvatarUrl(m.avatarUrl()); e.setCurrencyDefault(m.currencyDefault()); e.setTimezone(m.timezone()); e.setSkinId(m.skinId()); return toModel(repository.save(e)); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("User", id); repository.deleteById(id); }
    private UserModel toModel(User e) { return new UserModel(e.getId(), e.getEmail(), e.getPasswordHash(), e.getFullName(), e.getAvatarUrl(), e.getCurrencyDefault(), e.getTimezone(), e.getSkinId(), e.getCreatedAt(), e.getUpdatedAt()); }
}
