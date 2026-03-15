package com.trinetra.controller;

import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.AdminUserSummaryResponse;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Role;
import com.trinetra.repository.UserRepository;
import com.trinetra.service.ComplaintService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(complaintService.getAdminStats());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getRecentComplaints() {
        return ResponseEntity.ok(complaintService.getRecentComplaints());
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/complaints/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable UUID id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @PutMapping("/complaints/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(@RequestBody Map<String, String> payload) {
        String id = payload.get("id");
        String status = payload.get("status");
        if (id == null || id.isBlank() || status == null || status.isBlank()) {
            throw new BadRequestException("Both id and status are required");
        }
        ComplaintStatus complaintStatus = ComplaintStatus.from(status);
        return ResponseEntity.ok(complaintService.updateComplaintStatus(UUID.fromString(id), complaintStatus));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AdminUserSummaryResponse>> getUsers() {
        List<AdminUserSummaryResponse> users = userRepository.findAll().stream()
                .map(u -> AdminUserSummaryResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole() == null ? Role.EMPLOYEE : u.getRole())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(complaintService.getAdminAnalytics());
    }
}