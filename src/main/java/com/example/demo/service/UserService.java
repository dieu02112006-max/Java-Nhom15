package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<User> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public User findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    public User create(User user) { user.setId(null); if (user.getCurrencyDefault() == null) user.setCurrencyDefault("VND"); if (user.getTimezone() == null) user.setTimezone("Asia/Ho_Chi_Minh"); return repository.save(user); }
    public User update(Long id, User input) { User user = findById(id); user.setEmail(input.getEmail()); if (input.getPasswordHash() != null) user.setPasswordHash(input.getPasswordHash()); user.setFullName(input.getFullName()); user.setAvatarUrl(input.getAvatarUrl()); user.setCurrencyDefault(input.getCurrencyDefault() == null ? "VND" : input.getCurrencyDefault()); user.setTimezone(input.getTimezone() == null ? "Asia/Ho_Chi_Minh" : input.getTimezone()); user.setSkinId(input.getSkinId()); return repository.save(user); }
    public void delete(Long id) { if (!repository.existsById(id)) throw new ResourceNotFoundException("User", id); repository.deleteById(id); }
}
