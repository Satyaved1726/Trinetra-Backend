package com.trinetra.controller;

import com.trinetra.dto.LoginRequest;
import com.trinetra.dto.RegisterRequest;
import com.trinetra.model.AdminUser;
import com.trinetra.model.Role;
import com.trinetra.model.User;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import com.trinetra.security.JwtUtil;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }

            String name = request.getName() == null ? null : request.getName().trim();
            if (!StringUtils.hasText(name)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
            }

            if (userRepository.existsByEmail(email) || adminUserRepository.existsByUsernameIgnoreCase(email)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Email already registered"));
            }

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.EMPLOYEE);
            userRepository.save(user);

                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully"));
        } catch (DataIntegrityViolationException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        } catch (Exception e) {
                e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(email);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            if (!"ADMIN".equalsIgnoreCase(admin.getRole()) && !"SUPER_ADMIN".equalsIgnoreCase(admin.getRole())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }
            if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }

            String token = jwtUtil.generateToken(admin.getUsername());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "email", admin.getUsername(),
                    "role", admin.getRole()
            ));
        }

        Optional<User> employeeOpt = userRepository.findByEmail(email);
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        User employee = employeeOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(employee.getEmail());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", employee.getEmail(),
                "role", "EMPLOYEE"
        ));
    }
}