package com.trinetra.controller;

import com.trinetra.dto.AdminResponseRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.exception.BadRequestException;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.service.AdminService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> getReports(
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(adminService.getAllReports(parseStatus(status)));
    }

    @PutMapping("/report/{id}/status")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateStatus(id, request));
    }

    @PostMapping("/respond/{reportId}")
    public ResponseEntity<ReportResponse> respond(
            @PathVariable UUID reportId,
            @Valid @RequestBody AdminResponseRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(adminService.respondToReport(reportId, request, principal.getName()));
    }

    private ComplaintStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ComplaintStatus.from(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}