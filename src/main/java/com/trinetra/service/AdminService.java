package com.trinetra.service;

import com.trinetra.dto.AdminResponseRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Response;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.ResponseRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final ResponseRepository responseRepository;
    private final AdminUserRepository adminUserRepository;
    private final ReportService reportService;

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(ComplaintStatus status) {
        List<Complaint> complaints = status == null
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByStatus(status.name());

        return complaints.stream()
                .map(complaint -> reportService.toReportResponse(
                        complaint,
                        responseRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId())
                ))
                .toList();
    }

    @Transactional
    public ReportResponse updateStatus(UUID complaintId, StatusUpdateRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        complaint.setStatus(request.getStatus().name());
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return reportService.toReportResponse(
                updatedComplaint,
                responseRepository.findByComplaintIdOrderByCreatedAtAsc(updatedComplaint.getId())
        );
    }

    @Transactional
    public ReportResponse respondToReport(UUID complaintId, AdminResponseRequest request, String adminUsername) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        AdminUser admin = adminUserRepository.findByUsernameIgnoreCase(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        Response response = Response.builder()
                .complaint(complaint)
                .admin(admin)
                .message(request.getMessage().trim())
                .build();
        responseRepository.save(response);

        if (ComplaintStatus.SUBMITTED.name().equals(complaint.getStatus())) {
            complaint.setStatus(ComplaintStatus.UNDER_REVIEW.name());
        }
        complaint.setAdmin(admin);
        complaintRepository.save(complaint);

        return reportService.toReportResponse(
                complaint,
                responseRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId())
        );
    }
}