package com.example.demo.security;

import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .map(user -> new AppUserPrincipal(user, isActive(user.getId())))
                .orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không đúng"));
    }

    private boolean isActive(Long userId) {
        Boolean active = jdbc.queryForObject("SELECT is_active = 1 FROM users WHERE id = ?", Boolean.class, userId);
        return Boolean.TRUE.equals(active);
    }
}
