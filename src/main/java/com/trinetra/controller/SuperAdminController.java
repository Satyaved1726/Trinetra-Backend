package com.trinetra.controller;

import com.trinetra.dto.AdminAccountResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import com.trinetra.service.ComplaintService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/super-admin", "/api/superadmin"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComplaintService complaintService;

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password required"));
        }

        email = email.trim().toLowerCase();
        if (adminUserRepository.existsByUsernameIgnoreCase(email) || userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Admin already exists"));
        }

        AdminUser admin = AdminUser.builder()
                .username(email)
                .password(passwordEncoder.encode(password))
                .role("ADMIN")
                .build();

        adminUserRepository.save(admin);
        return ResponseEntity.ok(Map.of("message", "Admin created successfully"));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminAccountResponse>> getAdmins() {
        List<AdminAccountResponse> admins = adminUserRepository.findAllByRole("ADMIN")
                .stream()
            .sorted(java.util.Comparator.comparing(AdminUser::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(complaintService.getAdminAnalytics());
    }

    @PutMapping("/admin/{id}/disable")
    public ResponseEntity<AdminAccountResponse> disableAdmin(@PathVariable UUID id) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new BadRequestException("Only ADMIN accounts can be disabled");
        }

        admin.setRole("DISABLED_ADMIN");
        return ResponseEntity.ok(toResponse(adminUserRepository.save(admin)));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, String>> deleteAdmin(@PathVariable UUID id) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new BadRequestException("Only ADMIN accounts can be deleted");
        }

        adminUserRepository.delete(admin);
        return ResponseEntity.ok(Map.of("message", "Admin deleted successfully"));
    }

    private AdminAccountResponse toResponse(AdminUser admin) {
        return AdminAccountResponse.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .active(!"DISABLED_ADMIN".equalsIgnoreCase(admin.getRole()))
                .createdAt(null)
                .build();
    }
}
