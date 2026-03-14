package com.trinetra.controller;

import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.service.ComplaintService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ComplaintService complaintService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(complaintService.getAdminStats());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getRecentComplaints() {
        return ResponseEntity.ok(complaintService.getRecentComplaints());
    }
}