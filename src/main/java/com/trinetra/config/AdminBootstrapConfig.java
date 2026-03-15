package com.trinetra.config;

import com.trinetra.model.AdminUser;
import com.trinetra.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner bootstrapAdmin(
            @Value("${app.bootstrap-super-admin.username:superadmin@trinetra.com}") String adminUsername,
            @Value("${app.bootstrap-super-admin.password:SuperAdmin@123}") String adminPassword
    ) {
        return args -> {
            if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
                return;
            }

            if (adminUserRepository.findFirstByRoleIgnoreCase("SUPER_ADMIN").isPresent()) {
                return;
            }

            AdminUser admin = AdminUser.builder()
                    .username(adminUsername.trim().toLowerCase())
                    .password(passwordEncoder.encode(adminPassword))
                    .role("SUPER_ADMIN")
                    .active(true)
                    .build();
            adminUserRepository.save(admin);
        };
    }
}