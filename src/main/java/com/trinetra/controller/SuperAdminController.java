package com.trinetra.controller;

import com.trinetra.dto.AdminAccountResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.model.AdminUser;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.UserRepository;
import com.trinetra.service.ComplaintService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
        try {
            String email = Optional.ofNullable(request).map(r -> r.get("email")).orElse("");
            String password = Optional.ofNullable(request).map(r -> r.get("password")).orElse("");

            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                return errorResponse(new IllegalArgumentException("Email and password required"));
            }

            email = email.trim().toLowerCase();
            if (adminUserRepository.existsByUsernameIgnoreCase(email) || userRepository.existsByEmail(email)) {
                return errorResponse(new IllegalArgumentException("Admin already exists"));
            }

            AdminUser admin = AdminUser.builder()
                    .username(email)
                    .password(passwordEncoder.encode(password))
                    .role("ADMIN")
                    .build();

            adminUserRepository.save(admin);
            return successResponse(Map.of("message", "Admin created successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/admins")
    public ResponseEntity<?> getAdmins() {
        try {
            List<AdminAccountResponse> admins = Optional.ofNullable(adminUserRepository.findAllByRole("ADMIN"))
                    .orElse(List.of())
                    .stream()
                    .sorted(java.util.Comparator.comparing(AdminUser::getUsername, String.CASE_INSENSITIVE_ORDER))
                    .map(this::toResponse)
                    .toList();
            return successResponse(admins);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        try {
            AdminAnalyticsResponse analytics = sanitizeAnalytics(complaintService.getAdminAnalytics());
            return successResponse(analytics);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PutMapping("/admin/{id}/disable")
    public ResponseEntity<?> disableAdmin(@PathVariable UUID id) {
        try {
            Optional<AdminUser> adminOptional = adminUserRepository.findById(id);
            if (adminOptional.isEmpty()) {
                return errorResponse(new IllegalArgumentException("Admin not found"));
            }

            AdminUser admin = adminOptional.get();
            String role = Optional.ofNullable(admin.getRole()).orElse("");
            if (!"ADMIN".equalsIgnoreCase(role)) {
                return errorResponse(new IllegalArgumentException("Only ADMIN accounts can be disabled"));
            }

            admin.setRole("DISABLED_ADMIN");
            AdminAccountResponse response = toResponse(adminUserRepository.save(admin));
            return successResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable UUID id) {
        try {
            Optional<AdminUser> adminOptional = adminUserRepository.findById(id);
            if (adminOptional.isEmpty()) {
                return errorResponse(new IllegalArgumentException("Admin not found"));
            }

            AdminUser admin = adminOptional.get();
            String role = Optional.ofNullable(admin.getRole()).orElse("");
            if (!"ADMIN".equalsIgnoreCase(role)) {
                return errorResponse(new IllegalArgumentException("Only ADMIN accounts can be deleted"));
            }

            adminUserRepository.delete(admin);
            return successResponse(Map.of("message", "Admin deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    private AdminAccountResponse toResponse(AdminUser admin) {
        String role = Optional.ofNullable(admin).map(AdminUser::getRole).orElse("");
        return AdminAccountResponse.builder()
                .id(Optional.ofNullable(admin).map(AdminUser::getId).orElse(null))
                .username(Optional.ofNullable(admin).map(AdminUser::getUsername).orElse(""))
                .role(role)
                .active(!"DISABLED_ADMIN".equalsIgnoreCase(role))
                .createdAt(null)
                .build();
    }

    private AdminAnalyticsResponse sanitizeAnalytics(AdminAnalyticsResponse response) {
        if (response == null) {
            return AdminAnalyticsResponse.builder()
                    .totalComplaints(0)
                    .openComplaints(0)
                    .resolvedComplaints(0)
                    .anonymousComplaints(0)
                    .complaintsByCategory(Map.of())
                    .complaintsByStatus(Map.of())
                    .complaintsOverTime(Map.of())
                    .build();
        }

        return AdminAnalyticsResponse.builder()
                .totalComplaints(response.getTotalComplaints())
                .openComplaints(response.getOpenComplaints())
                .resolvedComplaints(response.getResolvedComplaints())
                .anonymousComplaints(response.getAnonymousComplaints())
                .complaintsByCategory(Optional.ofNullable(response.getComplaintsByCategory()).orElse(Map.of()))
                .complaintsByStatus(Optional.ofNullable(response.getComplaintsByStatus()).orElse(Map.of()))
                .complaintsOverTime(Optional.ofNullable(response.getComplaintsOverTime()).orElse(Map.of()))
                .build();
    }

    private ResponseEntity<Map<String, Object>> successResponse(Object data) {
        Object safeData = Optional.ofNullable(data).orElse(List.of());
        return ResponseEntity.ok(Map.of(
                "data", safeData,
                "message", "success"
        ));
    }

    private ResponseEntity<Map<String, Object>> errorResponse(Exception e) {
        return ResponseEntity.ok(Map.of(
                "data", List.of(),
                "error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")
        ));
    }
}
