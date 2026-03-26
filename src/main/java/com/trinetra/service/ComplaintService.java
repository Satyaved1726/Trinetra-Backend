package com.trinetra.service;

import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.dto.ComplaintTrackingResponse;
import com.trinetra.dto.EvidenceDTO;
import com.trinetra.dto.EvidenceFileResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ComplaintNotFoundException;
import com.trinetra.exception.UnauthorizedException;
import com.trinetra.exception.UserNotFoundException;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Evidence;
import com.trinetra.model.User;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.EvidenceRepository;
import com.trinetra.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;

    @Transactional
    public ComplaintSubmissionResponse submitComplaint(ComplaintRequest request, String userEmail) {
        boolean anonymous = Boolean.TRUE.equals(request.getIsAnonymous());
        ComplaintCategory complaintCategory = parseCategory(request.getCategory());
        UUID submittedByUserId = null;
        String anonymousToken = null;

        if (anonymous) {
            anonymousToken = UUID.randomUUID().toString();
        } else {
            if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication required for non-anonymous complaint submission");
            }
            User user = userRepository.findByEmail(normalizeEmail(userEmail))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
            submittedByUserId = user.getId();
        }

        Complaint complaint = Complaint.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
            .category(complaintCategory.name())
                .status(ComplaintStatus.SUBMITTED.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
            .userId(submittedByUserId)
            .anonymousToken(anonymousToken)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        List<EvidenceDTO> evidenceFiles = request.getEvidenceFiles();
        if (evidenceFiles != null) {
            for (EvidenceDTO file : evidenceFiles) {
                if (file == null || file.getUrl() == null || file.getUrl().isBlank()) {
                    continue;
                }
                String fileType = (file.getType() == null || file.getType().isBlank())
                        ? "application/octet-stream"
                        : file.getType().trim();

                Evidence evidence = Evidence.builder()
                    .complaint(saved)
                        .fileUrl(file.getUrl().trim())
                        .fileType(fileType)
                        .build();
                evidenceRepository.save(evidence);

                if (saved.getEvidenceUrl() == null || saved.getEvidenceUrl().isBlank()) {
                    saved.setEvidenceUrl(file.getUrl().trim());
                    saved = complaintRepository.save(saved);
                }
            }
        }

        return ComplaintSubmissionResponse.builder()
            .message("Complaint submitted")
                .trackingId(saved.getTrackingId())
            .anonymousToken(saved.getAnonymousToken())
                .build();
    }

    @Transactional
    public ComplaintSubmissionResponse submitAnonymousComplaint(ComplaintRequest request) {
        request.setIsAnonymous(true);
        return submitComplaint(request, null);
    }

    @Transactional(readOnly = true)
    public ComplaintTrackingResponse trackComplaint(String trackingId) {
        Complaint complaint = complaintRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        if (Boolean.TRUE.equals(complaint.getAnonymous())) {
            throw new UnauthorizedException("Anonymous token is required for this complaint");
        }

        return buildTrackingResponse(complaint);
    }

    @Transactional(readOnly = true)
    public ComplaintTrackingResponse trackComplaint(String trackingId, String anonymousToken) {
        Complaint complaint = complaintRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        if (!Boolean.TRUE.equals(complaint.getAnonymous())) {
            return buildTrackingResponse(complaint);
        }

        if (anonymousToken == null || anonymousToken.isBlank()) {
            throw new UnauthorizedException("anonymousToken is required for anonymous complaint tracking");
        }

        Complaint verified = complaintRepository.findByTrackingIdAndAnonymousToken(trackingId, anonymousToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid tracking credentials"));

        return buildTrackingResponse(verified);
    }

    private ComplaintTrackingResponse buildTrackingResponse(Complaint complaint) {
        return ComplaintTrackingResponse.builder()
                .trackingId(complaint.getTrackingId())
                .evidenceUrl(complaint.getEvidenceUrl())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(ComplaintCategory.from(complaint.getCategory()))
                .status(ComplaintStatus.from(complaint.getStatus()))
                .anonymous(Boolean.TRUE.equals(complaint.getAnonymous()))
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
        return complaintRepository.findAllWithEvidence()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID id) {
        Complaint complaint = complaintRepository.findByIdWithEvidence(id)
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
                        + complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW.name())
                        + complaintRepository.countByStatus(ComplaintStatus.INVESTIGATING.name())
                        + complaintRepository.countByStatus("SUBMITTED"))
                .resolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED.name()))
                .rejectedComplaints(complaintRepository.countByStatus(ComplaintStatus.REJECTED.name()))
                .build();
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics() {
        List<Complaint> complaints = complaintRepository.findAll();

        Map<String, Long> monthlyComplaints = complaints.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().getYear() + "-" + String.format("%02d", c.getCreatedAt().getMonthValue()),
                        Collectors.counting()
                ));

        Map<String, Long> categoryStats = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));

        long resolved = complaints.stream()
                .filter(c -> ComplaintStatus.RESOLVED.name().equalsIgnoreCase(c.getStatus()))
                .count();

        return AdminAnalyticsResponse.builder()
                .totalComplaints(complaints.size())
                .resolvedComplaints(resolved)
                .monthlyComplaints(monthlyComplaints)
                .categoryStats(categoryStats)
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
        List<EvidenceFileResponse> evidenceFiles = complaint.getEvidenceFiles() == null
            ? List.of()
            : complaint.getEvidenceFiles().stream()
            .map(evidence -> EvidenceFileResponse.builder()
                .id(evidence.getId())
                .fileUrl(evidence.getFileUrl())
                .fileType(evidence.getFileType())
                .uploadedAt(evidence.getUploadedAt())
                .build())
            .toList();

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .trackingId(complaint.getTrackingId())
                .evidenceUrl(complaint.getEvidenceUrl())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(ComplaintCategory.from(complaint.getCategory()))
                .status(ComplaintStatus.from(complaint.getStatus()))
                .createdAt(complaint.getCreatedAt())
                .anonymous(Boolean.TRUE.equals(complaint.getAnonymous()))
                .userId(complaint.getUserId())
                .createdBy(complaint.getUserId())
                .adminId(complaint.getAdmin() != null ? complaint.getAdmin().getId() : null)
                .evidenceFiles(evidenceFiles)
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

    private ComplaintCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BadRequestException("Category is required");
        }
        try {
            return ComplaintCategory.from(category.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}