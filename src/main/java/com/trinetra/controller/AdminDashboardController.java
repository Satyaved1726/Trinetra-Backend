package com.trinetra.controller;

import com.trinetra.dto.AdminAssignRequest;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.AdminComplaintsPageResponse;
import com.trinetra.dto.AdminNotificationResponse;
import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.AdminUsersPageResponse;
import com.trinetra.dto.AuditLogResponse;
import com.trinetra.dto.ComplaintNoteRequest;
import com.trinetra.dto.ComplaintNoteResponse;
import com.trinetra.dto.ComplaintTimelineEventResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.dto.UserBlockResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.service.AdminManagementService;
import com.trinetra.service.ComplaintService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminDashboardController {

    private final ComplaintService complaintService;
    private final AdminManagementService adminManagementService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(complaintService.getAdminStats());
    }

    @GetMapping("/complaints")
    public ResponseEntity<AdminComplaintsPageResponse> getAllComplaints(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminManagementService.getComplaints(status, category, search, fromDate, toDate, page, size));
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminManagementService.updateComplaintStatus(id, request.getStatus(), principal.getName()));
    }

    @PutMapping("/complaints/{id}/assign")
    public ResponseEntity<?> assignComplaint(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminAssignRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminManagementService.assignComplaint(id, request, principal.getName()));
    }

    @GetMapping("/complaints/{id}/timeline")
    public ResponseEntity<List<ComplaintTimelineEventResponse>> getTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.getTimeline(id));
    }

    @PostMapping("/complaints/{id}/notes")
    public ResponseEntity<ComplaintNoteResponse> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintNoteRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminManagementService.addNote(id, request, principal.getName()));
    }

    @GetMapping("/complaints/{id}/notes")
    public ResponseEntity<List<ComplaintNoteResponse>> getNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.getNotes(id));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<AdminNotificationResponse>> getNotifications() {
        return ResponseEntity.ok(adminManagementService.getNotifications());
    }

    @GetMapping("/users")
    public ResponseEntity<AdminUsersPageResponse> getUsers() {
        return ResponseEntity.ok(adminManagementService.getUsers());
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<UserBlockResponse> blockUser(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(adminManagementService.blockUser(id, principal.getName()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminManagementService.getAuditLogs(page, size));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminManagementService.getAnalytics());
    }

    @PutMapping("/complaints/status")
    public ResponseEntity<?> updateComplaintStatusLegacy(@RequestBody Map<String, String> payload, Principal principal) {
        String id = payload.get("id");
        String status = payload.get("status");
        if (id == null || id.isBlank() || status == null || status.isBlank()) {
            throw new BadRequestException("Both id and status are required");
        }
        ComplaintStatus complaintStatus = ComplaintStatus.from(status);
        return ResponseEntity.ok(adminManagementService.updateComplaintStatus(UUID.fromString(id), complaintStatus, principal.getName()));
    }
}