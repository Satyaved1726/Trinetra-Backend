package com.trinetra.controller;

import com.trinetra.dto.ReportRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.User;
import com.trinetra.repository.UserRepository;
import com.trinetra.service.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> submitReport(
            @Valid @RequestPart("request") ReportRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Principal principal
    ) {
        UUID authenticatedUserId = resolveUserId(principal);
        ReportResponse response = reportService.submitReport(request, authenticatedUserId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/status/{token}")
    public ResponseEntity<ReportResponse> trackReport(@PathVariable String token) {
        return ResponseEntity.ok(reportService.getReportStatus(token));
    }

    private UUID resolveUserId(Principal principal) {
        if (principal == null) {
            return null;
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return user.getId();
    }

    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> submitReportWithoutFiles(
            @Valid @org.springframework.web.bind.annotation.RequestBody ReportRequest request,
            Principal principal
    ) {
        UUID authenticatedUserId = resolveUserId(principal);
        ReportResponse response = reportService.submitReport(request, authenticatedUserId, List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}