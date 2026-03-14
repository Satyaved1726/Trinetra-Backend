package com.trinetra.service;

import com.trinetra.dto.AdminResponseRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Response;
import com.trinetra.model.User;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.ResponseRepository;
import com.trinetra.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ReportService reportService;

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(ComplaintStatus status) {
        List<Complaint> complaints = status == null
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByStatus(status);

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
        complaint.setStatus(request.getStatus());
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return reportService.toReportResponse(
                updatedComplaint,
                responseRepository.findByComplaintIdOrderByCreatedAtAsc(updatedComplaint.getId())
        );
    }

    @Transactional
    public ReportResponse respondToReport(UUID complaintId, AdminResponseRequest request, String adminEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        Response response = Response.builder()
                .complaint(complaint)
                .admin(admin)
                .message(request.getMessage().trim())
                .build();
        responseRepository.save(response);

        if (complaint.getStatus() == ComplaintStatus.SUBMITTED) {
            complaint.setStatus(ComplaintStatus.UNDER_REVIEW);
            complaintRepository.save(complaint);
        }

        return reportService.toReportResponse(
                complaint,
                responseRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId())
        );
    }
}