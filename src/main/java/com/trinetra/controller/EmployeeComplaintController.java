package com.trinetra.controller;

import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.service.ComplaintService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(value = "/complaints", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintSubmissionResponse> submitComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Principal principal
    ) {
        ComplaintSubmissionResponse response = complaintService.submitComplaint(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/complaints", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitComplaintMultipart(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam(defaultValue = "false") boolean anonymous,
            @RequestParam(required = false) MultipartFile evidence,
            Principal principal
    ) {
        Map<String, String> response = complaintService.submitComplaintMultipart(
                title,
                description,
                category,
                anonymous,
                evidence,
                principal.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-complaints")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Principal principal) {
        return ResponseEntity.ok(complaintService.getUserComplaints(principal.getName()));
    }
}
