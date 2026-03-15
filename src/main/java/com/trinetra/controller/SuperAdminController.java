package com.trinetra.controller;

import com.trinetra.dto.AdminAccountResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.CreateAdminRequest;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import com.trinetra.service.ComplaintService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComplaintService complaintService;

    @PostMapping("/create-admin")
    public ResponseEntity<AdminAccountResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Name is required");
        }
        if (adminUserRepository.existsByUsernameIgnoreCase(email) || userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        AdminUser admin = AdminUser.builder()
                .username(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .build();

        AdminUser saved = adminUserRepository.save(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminAccountResponse>> getAdmins() {
        List<AdminAccountResponse> admins = adminUserRepository.findAllByRoleIgnoreCase("ADMIN")
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
                .email(admin.getUsername())
                .role(admin.getRole())
            .active(!"DISABLED_ADMIN".equalsIgnoreCase(admin.getRole()))
            .createdAt(null)
                .build();
    }
}
