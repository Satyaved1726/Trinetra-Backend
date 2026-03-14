package com.trinetra.service;

import com.trinetra.dto.AuthResponse;
import com.trinetra.dto.LoginRequest;
import com.trinetra.dto.RegisterRequest;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.InvalidLoginException;
import com.trinetra.exception.UserNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.model.Role;
import com.trinetra.model.User;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import com.trinetra.security.JwtUtil;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)
                .build();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return buildAuthResponse(user, userDetails);
    }

    public AuthResponse login(LoginRequest request) {
        String rawIdentifier = request.getEmail().trim();

        // Check if this is an admin login (by username in admin_user table)
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(rawIdentifier);
        String authIdentifier = adminOpt.map(AdminUser::getUsername).orElse(rawIdentifier.toLowerCase());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authIdentifier, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new InvalidLoginException("Invalid credentials");
        }

        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(admin.getUsername());
            return AuthResponse.builder()
                    .jwtToken(jwtUtil.generateToken(userDetails))
                    .role(Role.ADMIN)
                    .userId(admin.getId().toString())
                    .build();
        }

        String normalizedEmail = rawIdentifier.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return buildAuthResponse(user, userDetails);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails userDetails) {
        return AuthResponse.builder()
                .jwtToken(jwtUtil.generateToken(userDetails))
                .role(user.getRole())
                .userId(user.getId().toString())
                .build();
    }
}