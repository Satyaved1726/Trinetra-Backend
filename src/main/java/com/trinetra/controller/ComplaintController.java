package com.trinetra.controller;

import com.trinetra.dto.AdminAssignRequest;
import com.trinetra.dto.AdminComplaintsPageResponse;
import com.trinetra.dto.ComplaintAssignRequest;
import com.trinetra.dto.ComplaintNoteRequest;
import com.trinetra.dto.ComplaintNoteResponse;
import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.dto.ComplaintTrackingResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.dto.TrackComplaintRequest;
import com.trinetra.exception.BadRequestException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AdminManagementService adminManagementService;

    // POST /api/complaints — authenticated submit (employees or anonymous)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintSubmissionResponse> submitComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Principal principal
    ) {
        log.info(
            "Complaint payload received on /api/complaints: title='{}', category='{}', isAnonymous={}, evidenceCount={}",
            request.getTitle(),
            request.getCategory(),
            request.getIsAnonymous(),
            request.getEvidenceFiles() == null ? 0 : request.getEvidenceFiles().size()
        );
        ComplaintSubmissionResponse response = principal != null
                ? complaintService.submitComplaint(request, principal.getName())
                : complaintService.submitAnonymousComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/complaints/submit — JSON submit with evidence URLs
    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintSubmissionResponse> submitComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication
    ) {
        log.info(
            "Complaint payload received on /api/complaints/submit: title='{}', category='{}', isAnonymous={}, evidenceCount={}",
            request.getTitle(),
            request.getCategory(),
            request.getIsAnonymous(),
            request.getEvidenceFiles() == null ? 0 : request.getEvidenceFiles().size()
        );
        String userEmail = authentication != null ? authentication.getName() : null;
        ComplaintSubmissionResponse response = complaintService.submitComplaint(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/complaints/anonymous — anonymous submit
    @PostMapping("/anonymous")
    public ResponseEntity<ComplaintSubmissionResponse> submitAnonymousComplaint(
            @Valid @RequestBody ComplaintRequest request
    ) {
        ComplaintSubmissionResponse response = complaintService.submitAnonymousComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/complaints — list complaints with filters (admin/officer)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OFFICER')")
    public ResponseEntity<AdminComplaintsPageResponse> getAllComplaints(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminManagementService.getComplaints(status, category, priority, search, fromDate, toDate, page, size));
    }

    // GET /api/complaints/all — list all complaints (admin, legacy path)
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaintsLegacy() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // GET /api/complaints/{id} — get single complaint by UUID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OFFICER', 'EMPLOYEE')")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.getComplaintDetails(id));
    }

    // GET /api/complaints/track/{trackingId} — public tracking
    @GetMapping("/track/{trackingId}")
    public ResponseEntity<ComplaintTrackingResponse> trackComplaint(@PathVariable String trackingId) {
        return ResponseEntity.ok(complaintService.trackComplaint(trackingId));
    }

    // POST /api/complaints/track — anonymous token based tracking
    @PostMapping("/track")
    public ResponseEntity<ComplaintTrackingResponse> trackComplaint(@Valid @RequestBody TrackComplaintRequest request) {
        return ResponseEntity.ok(complaintService.trackComplaint(request.getTrackingId(), request.getAnonymousToken()));
    }

    // GET /api/complaints/my — employee's own complaints
    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Principal principal) {
        return ResponseEntity.ok(complaintService.getUserComplaints(principal.getName()));
    }

    // PUT /api/complaints/{id}/status — update status (admin)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OFFICER')")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Principal principal
    ) {
        try {
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            ComplaintResponse updated = adminManagementService.updateComplaintStatus(id, request.getStatus(), actor);
            return ResponseEntity.ok(Map.of(
                    "data", Optional.ofNullable(updated).orElse(List.of()),
                    "message", "success"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                    "data", List.of(),
                    "error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")
            ));
        }
    }

    // PUT /api/complaints/{id}/assign — assign complaint to officer/admin
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintAssignRequest request,
            Principal principal
    ) {
        AdminAssignRequest payload = new AdminAssignRequest();
        payload.setAssignedTo(request.getAssignedTo());
        return ResponseEntity.ok(adminManagementService.assignComplaint(id, payload, principal.getName()));
    }

    // POST /api/complaints/{id}/notes — add complaint note
    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OFFICER')")
    public ResponseEntity<ComplaintNoteResponse> addComplaintNote(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintNoteRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminManagementService.addNote(id, request, principal.getName()));
    }

    // GET /api/complaints/{id}/notes — list complaint notes
    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OFFICER')")
    public ResponseEntity<List<ComplaintNoteResponse>> getComplaintNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(adminManagementService.getNotes(id));
    }

    // PUT /api/complaints/status/{id} — legacy path
    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> updateComplaintStatusLegacy(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload,
            Principal principal
    ) {
        String statusValue = payload.get("status");
        if (statusValue == null || statusValue.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        ComplaintStatus status = ComplaintStatus.from(statusValue);
        return ResponseEntity.ok(adminManagementService.updateComplaintStatus(id, status, principal.getName()));
    }

    // DELETE /api/complaints/{id} — delete complaint (admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteComplaint(@PathVariable UUID id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
}
