package com.trinetra.security;

import com.trinetra.model.AdminUser;
import com.trinetra.model.User;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Check admin_user table by username first
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(identifier);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            String role = admin.getRole() == null ? "" : admin.getRole().toUpperCase();
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                throw new UsernameNotFoundException("Admin account is inactive");
            }
            return org.springframework.security.core.userdetails.User.builder()
                    .username(admin.getUsername())
                    .password(admin.getPassword())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                    .build();
        }

        // Fall back to users table by email
        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}