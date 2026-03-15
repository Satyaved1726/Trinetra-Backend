package com.trinetra.controller;

import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.dto.ComplaintTrackingResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.service.ComplaintService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // POST /api/complaints — authenticated submit (employees or anonymous)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintSubmissionResponse> submitComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Principal principal
    ) {
        ComplaintSubmissionResponse response = principal != null
                ? complaintService.submitComplaint(request, principal.getName())
                : complaintService.submitAnonymousComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/complaints — multipart submit with optional evidence
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitComplaintWithEvidence(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam boolean anonymous,
            @RequestParam(required = false) MultipartFile evidence,
            Principal principal
    ) {
        String userEmail = principal != null ? principal.getName() : null;
        Map<String, String> response = complaintService.submitComplaintMultipart(
                title,
                description,
                category,
                anonymous,
                evidence,
                userEmail
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/complaints/submit — authenticated employee submit (legacy)
    @PostMapping("/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ComplaintSubmissionResponse> submitComplaintLegacy(
            @Valid @RequestBody ComplaintRequest request,
            Principal principal
    ) {
        ComplaintSubmissionResponse response = complaintService.submitComplaint(request, principal.getName());
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

    // POST /api/complaints/anonymous — multipart submit with optional evidence
    @PostMapping(value = "/anonymous", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitAnonymousComplaintWithEvidence(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam(required = false, defaultValue = "true") boolean anonymous,
            @RequestParam(required = false) MultipartFile evidence
    ) {
        Map<String, String> response = complaintService.submitComplaintMultipart(
                title,
                description,
                category,
                anonymous,
                evidence,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/complaints/upload — upload evidence file and return file URL
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadEvidence(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID complaintId
    ) {
        Map<String, String> response = complaintService.uploadEvidence(file, complaintId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/complaints — list all complaints (admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // GET /api/complaints/all — list all complaints (admin, legacy path)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaintsLegacy() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // GET /api/complaints/{id} — get single complaint by UUID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable UUID id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    // GET /api/complaints/track/{trackingId} — public tracking
    @GetMapping("/track/{trackingId}")
    public ResponseEntity<ComplaintTrackingResponse> trackComplaint(@PathVariable String trackingId) {
        return ResponseEntity.ok(complaintService.trackComplaint(trackingId));
    }

    // GET /api/complaints/my — employee's own complaints
    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Principal principal) {
        return ResponseEntity.ok(complaintService.getUserComplaints(principal.getName()));
    }

    // PUT /api/complaints/{id}/status — update status (admin)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload
    ) {
        String statusValue = payload.get("status");
        if (statusValue == null || statusValue.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        ComplaintStatus status = ComplaintStatus.from(statusValue);
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status));
    }

    // PUT /api/complaints/status/{id} — legacy path
    @PutMapping("/status/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> updateComplaintStatusLegacy(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload
    ) {
        String statusValue = payload.get("status");
        if (statusValue == null || statusValue.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        ComplaintStatus status = ComplaintStatus.from(statusValue);
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status));
    }

    // DELETE /api/complaints/{id} — delete complaint (admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteComplaint(@PathVariable UUID id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
}
