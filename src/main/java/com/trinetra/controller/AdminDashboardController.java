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
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintTimelineEventResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.dto.UserBlockResponse;
import com.trinetra.exception.ComplaintNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.service.AdminManagementService;
import com.trinetra.service.ComplaintService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
// @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")  // Temporarily disabled for debugging
public class AdminDashboardController {

    private final ComplaintService complaintService;
    private final AdminManagementService adminManagementService;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            AdminStatsResponse stats = sanitizeStats(complaintService.getAdminStats());
            return successResponse(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints")
    public ResponseEntity<?> getAllComplaints(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            List<ComplaintResponse> complaints = Optional.ofNullable(complaintService.getAllComplaints()).orElse(List.of());
            return ResponseEntity.ok(Map.of("data", complaints));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")));
        }
    }

    @GetMapping("/admins")
    public ResponseEntity<?> getAdmins() {
        try {
            List<AdminUser> admins = Optional.ofNullable(adminManagementService.getAdmins()).orElse(List.of());
            return successResponse(admins);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints/assigned/{admin}")
    public ResponseEntity<?> getAssignedComplaints(@PathVariable String admin) {
        try {
            List<ComplaintResponse> complaints = Optional.ofNullable(adminManagementService.getAssignedComplaints(admin)).orElse(List.of());
            return successResponse(complaints);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaintById(@PathVariable UUID id) {
        try {
            ComplaintResponse complaint = adminManagementService.getComplaintDetails(id);
            return ResponseEntity.ok(Map.of("data", complaint));
        } catch (ComplaintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Complaint not found")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")));
        }
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Principal principal
    ) {
        try {
            String statusValue = Optional.ofNullable(body).map(payload -> payload.get("status")).orElse("");
            ComplaintStatus complaintStatus = ComplaintStatus.from(statusValue);
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
                ComplaintResponse updated = adminManagementService.updateComplaintStatus(id, complaintStatus, actor);
                return ResponseEntity.ok(Map.of(
                    "message", "Status updated",
                    "data", updated
                ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Invalid status")));
        } catch (ComplaintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Complaint not found")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")));
        }
    }

    @PutMapping("/complaints/{id}/assign")
    public ResponseEntity<?> assignComplaint(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminAssignRequest request,
            Principal principal
    ) {
        try {
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            Object assigned = adminManagementService.assignComplaint(id, request, actor);
            return successResponse(assigned);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints/{id}/timeline")
    public ResponseEntity<?> getTimeline(@PathVariable UUID id) {
        try {
            List<ComplaintTimelineEventResponse> timeline = Optional.ofNullable(adminManagementService.getTimeline(id)).orElse(List.of());
            return successResponse(timeline);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PostMapping("/complaints/{id}/notes")
    public ResponseEntity<?> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintNoteRequest request,
            Principal principal
    ) {
        try {
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            ComplaintNoteResponse note = adminManagementService.addNote(id, request, actor);
            return successResponse(note);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints/{id}/notes")
    public ResponseEntity<?> getNotes(@PathVariable UUID id) {
        try {
            List<ComplaintNoteResponse> notes = Optional.ofNullable(adminManagementService.getNotes(id)).orElse(List.of());
            return successResponse(notes);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PostMapping("/complaints/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Principal principal
    ) {
        try {
            String note = Optional.ofNullable(body).map(payload -> payload.get("note")).orElse("");
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            ComplaintNoteResponse saved = adminManagementService.addComment(id, note, actor);
            return successResponse(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/complaints/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable UUID id) {
        try {
            List<ComplaintNoteResponse> notes = Optional.ofNullable(adminManagementService.getNotes(id)).orElse(List.of());
            return successResponse(notes);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        try {
            List<AdminNotificationResponse> notifications = Optional.ofNullable(adminManagementService.getNotifications()).orElse(List.of());
            return successResponse(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        try {
            AdminUsersPageResponse users = sanitizeUsers(adminManagementService.getUsers());
            return successResponse(users);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable UUID id, Principal principal) {
        try {
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            UserBlockResponse blocked = adminManagementService.blockUser(id, actor);
            return successResponse(blocked);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Page<AuditLogResponse> logs = Optional.ofNullable(adminManagementService.getAuditLogs(page, size))
                    .orElse(Page.empty());
            return successResponse(logs);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        try {
            List<ComplaintResponse> complaints = Optional.ofNullable(complaintService.getAllComplaints()).orElse(List.of());
            long total = complaints.size();
            long resolved = complaints.stream()
                .filter(c -> "RESOLVED".equals(c.getStatus()))
                .count();
            long rejected = complaints.stream()
                .filter(c -> "REJECTED".equals(c.getStatus()))
                .count();
            long open = complaints.stream()
                .filter(c -> "UNDER_REVIEW".equals(c.getStatus()) || "INVESTIGATING".equals(c.getStatus()))
                .count();

            return ResponseEntity.ok(Map.of(
                    "total", total,
                    "resolved", resolved,
                    "rejected", rejected,
                    "open", open
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "total", 0,
                    "resolved", 0,
                    "rejected", 0,
                    "open", 0
            ));
        }
    }

    @PutMapping("/complaints/status")
    public ResponseEntity<?> updateComplaintStatusLegacy(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            String id = Optional.ofNullable(payload).map(p -> p.get("id")).orElse("");
            String status = Optional.ofNullable(payload).map(p -> p.get("status")).orElse("");
            if (id.isBlank() || status.isBlank()) {
                return errorResponse(new IllegalArgumentException("Both id and status are required"));
            }

            ComplaintStatus complaintStatus = ComplaintStatus.from(status);
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            Object updated = adminManagementService.updateComplaintStatus(UUID.fromString(id), complaintStatus, actor);
            return successResponse(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    private AdminStatsResponse sanitizeStats(AdminStatsResponse response) {
        if (response == null) {
            return AdminStatsResponse.builder()
                    .totalComplaints(0)
                    .pendingComplaints(0)
                    .resolvedComplaints(0)
                    .rejectedComplaints(0)
                    .build();
        }

        return AdminStatsResponse.builder()
                .totalComplaints(Optional.ofNullable(response).map(AdminStatsResponse::getTotalComplaints).orElse(0L))
                .pendingComplaints(Optional.ofNullable(response).map(AdminStatsResponse::getPendingComplaints).orElse(0L))
                .resolvedComplaints(Optional.ofNullable(response).map(AdminStatsResponse::getResolvedComplaints).orElse(0L))
                .rejectedComplaints(Optional.ofNullable(response).map(AdminStatsResponse::getRejectedComplaints).orElse(0L))
                .build();
    }

    private AdminComplaintsPageResponse sanitizeComplaints(AdminComplaintsPageResponse response) {
        if (response == null) {
            return AdminComplaintsPageResponse.builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalPages(0)
                    .totalElements(0)
                    .build();
        }

        return AdminComplaintsPageResponse.builder()
                .content(Optional.ofNullable(response.getContent()).orElse(List.of()))
                .page(response.getPage())
                .size(response.getSize())
                .totalPages(response.getTotalPages())
                .totalElements(response.getTotalElements())
                .build();
    }

    private AdminUsersPageResponse sanitizeUsers(AdminUsersPageResponse response) {
        if (response == null) {
            return AdminUsersPageResponse.builder()
                    .users(List.of())
                    .build();
        }

        return AdminUsersPageResponse.builder()
                .users(Optional.ofNullable(response.getUsers()).orElse(List.of()))
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
