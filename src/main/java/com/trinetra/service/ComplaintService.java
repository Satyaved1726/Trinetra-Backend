package com.trinetra.service;

import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.dto.ComplaintTrackingResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ComplaintNotFoundException;
import com.trinetra.exception.UserNotFoundException;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.User;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    @Transactional
    public ComplaintSubmissionResponse submitComplaint(ComplaintRequest request, String userEmail) {
        User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());

        Complaint complaint = Complaint.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory().name())
                .status(ComplaintStatus.SUBMITTED.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
                .userId(anonymous ? null : user.getId())
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return ComplaintSubmissionResponse.builder()
                .message("Complaint submitted successfully")
                .trackingId(saved.getTrackingId())
                .build();
    }

    @Transactional
    public ComplaintSubmissionResponse submitAnonymousComplaint(ComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory().name())
                .status(ComplaintStatus.SUBMITTED.name())
                .anonymous(true)
                .trackingId(generateTrackingId())
                .userId(null)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return ComplaintSubmissionResponse.builder()
                .message("Complaint submitted successfully")
                .trackingId(saved.getTrackingId())
                .build();
    }

    @Transactional(readOnly = true)
    public ComplaintTrackingResponse trackComplaint(String trackingId) {
        Complaint complaint = complaintRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found for tracking ID"));

        return ComplaintTrackingResponse.builder()
                .trackingId(complaint.getTrackingId())
                .title(complaint.getTitle())
                .category(ComplaintCategory.from(complaint.getCategory()))
                .status(ComplaintStatus.from(complaint.getStatus()))
                .createdAt(complaint.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getUserComplaints(String userEmail) {
        User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return complaintRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));
        return toResponse(complaint);
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(UUID complaintId, ComplaintStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));
        complaint.setStatus(status.name());
        return toResponse(complaintRepository.save(complaint));
    }

    @Transactional
    public void deleteComplaint(UUID id) {
        if (!complaintRepository.existsById(id)) {
            throw new ComplaintNotFoundException("Complaint not found");
        }
        complaintRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        return AdminStatsResponse.builder()
                .totalComplaints(complaintRepository.count())
                .pendingComplaints(complaintRepository.countByStatus(ComplaintStatus.SUBMITTED.name())
                        + complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW.name()))
                .resolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED.name()))
                .rejectedComplaints(complaintRepository.countByStatus(ComplaintStatus.REJECTED.name()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getRecentComplaints() {
        return complaintRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .trackingId(complaint.getTrackingId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(ComplaintCategory.from(complaint.getCategory()))
                .status(ComplaintStatus.from(complaint.getStatus()))
                .createdAt(complaint.getCreatedAt())
                .anonymous(complaint.isAnonymous())
                .userId(complaint.getUserId())
                .adminId(complaint.getAdmin() != null ? complaint.getAdmin().getId() : null)
                .build();
    }

    private String generateTrackingId() {
        int year = LocalDateTime.now().getYear();
        String prefix = "TRI-" + year + "-";
        long count = complaintRepository.countByTrackingIdStartingWith(prefix);
        String candidate = prefix + String.format("%04d", count + 1);
        while (complaintRepository.existsByTrackingId(candidate)) {
            count++;
            candidate = prefix + String.format("%04d", count + 1);
        }
        return candidate;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}