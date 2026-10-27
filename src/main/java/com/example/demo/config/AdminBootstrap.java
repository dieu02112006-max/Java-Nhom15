package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    ApplicationRunner createConfiguredAdmin(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String configuredEmail,
            @Value("${app.admin.password:}") String configuredPassword) {
        return args -> {
            String email = configuredEmail == null ? "" : configuredEmail.trim().toLowerCase();
            if (email.isBlank() && (configuredPassword == null || configuredPassword.isBlank())) {
                log.info("No bootstrap admin configured. Set DAYFLOW_ADMIN_EMAIL and DAYFLOW_ADMIN_PASSWORD to create one.");
                return;
            }
            if (email.isBlank() || configuredPassword == null || configuredPassword.length() < 12) {
                throw new IllegalStateException("Configure both DAYFLOW_ADMIN_EMAIL and DAYFLOW_ADMIN_PASSWORD (at least 12 characters)");
            }
            if (users.existsByEmail(email)) {
                log.info("Configured admin email already exists; leaving its role and credentials unchanged.");
                return;
            }
            users.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(configuredPassword))
                    .fullName("Dayflow Administrator")
                    .role(UserRole.ADMIN)
                    .build());
            log.info("Created the configured Dayflow administrator account.");
        };
    }
}
