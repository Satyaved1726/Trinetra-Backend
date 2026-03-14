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
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.User;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private static final String TRACKING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
                .category(request.getCategory())
                .status(ComplaintStatus.SUBMITTED)
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
                .category(request.getCategory())
                .status(ComplaintStatus.SUBMITTED)
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
                .category(complaint.getCategory())
                .status(complaint.getStatus())
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

    @Transactional
    public ComplaintResponse updateComplaintStatus(UUID complaintId, ComplaintStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));
        complaint.setStatus(status);
        return toResponse(complaintRepository.save(complaint));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        return AdminStatsResponse.builder()
                .totalComplaints(complaintRepository.count())
                .pendingComplaints(complaintRepository.countByStatus(ComplaintStatus.SUBMITTED)
                        + complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW))
                .resolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED))
                .rejectedComplaints(complaintRepository.countByStatus(ComplaintStatus.REJECTED))
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
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .anonymous(complaint.isAnonymous())
                .build();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new UserNotFoundException("Authenticated user email is missing");
        }
        return email.trim().toLowerCase();
    }

    private String generateTrackingId() {
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder randomSix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                randomSix.append(TRACKING_CHARS.charAt(RANDOM.nextInt(TRACKING_CHARS.length())));
            }
            String trackingId = "TRN-" + randomSix;
            if (!complaintRepository.existsByTrackingId(trackingId)) {
                return trackingId;
            }
        }
        throw new IllegalStateException("Unable to generate a unique complaint tracking ID");
    }
}