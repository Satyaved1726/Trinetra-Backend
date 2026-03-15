package com.trinetra.service;

import com.trinetra.dto.AdminStatsResponse;
import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.ComplaintRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintSubmissionResponse;
import com.trinetra.dto.ComplaintTrackingResponse;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EvidenceRepository evidenceRepository;

    @Transactional
    public ComplaintSubmissionResponse submitComplaint(ComplaintRequest request, String userEmail) {
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
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
                .category(request.getCategory().name())
                .status(ComplaintStatus.PENDING.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
            .userId(submittedByUserId)
            .anonymousToken(anonymousToken)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return ComplaintSubmissionResponse.builder()
            .message("Complaint submitted")
                .trackingId(saved.getTrackingId())
            .anonymousToken(saved.getAnonymousToken())
                .build();
    }

    @Transactional
    public ComplaintSubmissionResponse submitAnonymousComplaint(ComplaintRequest request) {
        request.setAnonymous(true);
        return submitComplaint(request, null);
    }

    @Transactional
    public ComplaintSubmissionResponse submitComplaintWithFiles(
            ComplaintRequest request,
            List<MultipartFile> files,
            String userEmail) {
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
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
                .category(request.getCategory().name())
                .status(ComplaintStatus.PENDING.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
                .userId(submittedByUserId)
                .anonymousToken(anonymousToken)
                .build();

        complaint = complaintRepository.save(complaint);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                Map<String, String> stored = fileStorageService.storeFile(file);
                Evidence evidence = Evidence.builder()
                        .complaint(complaint)
                        .fileUrl(stored.get("fileUrl"))
                        .fileType(stored.get("fileType"))
                        .build();
                evidenceRepository.save(evidence);
                if (complaint.getEvidenceUrl() == null) {
                    complaint.setEvidenceUrl(stored.get("fileUrl"));
                    complaintRepository.save(complaint);
                }
            }
        }

        return ComplaintSubmissionResponse.builder()
                .message("Complaint submitted successfully")
                .trackingId(complaint.getTrackingId())
                .anonymousToken(complaint.getAnonymousToken())
                .build();
    }

    @Transactional
    public Map<String, String> submitComplaintMultipart(
            String title,
            String description,
            String category,
            boolean anonymous,
            MultipartFile evidence,
            String userEmail
    ) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (category == null || category.isBlank()) {
            throw new BadRequestException("Category is required");
        }

        ComplaintCategory complaintCategory;
        try {
            complaintCategory = ComplaintCategory.from(category.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        UUID userId = null;
        if (!anonymous && userEmail != null && !userEmail.isBlank()) {
            User user = userRepository.findByEmail(normalizeEmail(userEmail))
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            userId = user.getId();
        }

        Complaint complaint = Complaint.builder()
                .title(title.trim())
                .description(description.trim())
                .category(complaintCategory.name())
                .status(ComplaintStatus.PENDING.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
            .userId(anonymous ? null : userId)
            .anonymousToken(anonymous ? UUID.randomUUID().toString() : null)
                .build();

        if (evidence != null && !evidence.isEmpty()) {
            Map<String, String> stored = fileStorageService.storeFile(evidence);
            String fileUrl = stored.get("fileUrl");
            complaint.setEvidenceUrl(fileUrl);
            complaint = complaintRepository.save(complaint);
            Evidence evidenceRow = Evidence.builder()
                    .complaint(complaint)
                    .fileUrl(fileUrl)
                    .fileType(stored.get("fileType"))
                    .build();
            evidenceRepository.save(evidenceRow);
        } else {
            complaint = complaintRepository.save(complaint);
        }

        return Map.of(
                "trackingId", complaint.getTrackingId(),
            "anonymousToken", complaint.getAnonymousToken() == null ? "" : complaint.getAnonymousToken(),
                "message", "Complaint submitted successfully"
        );
    }

    @Transactional
    public Map<String, String> uploadEvidence(MultipartFile file, UUID complaintId) {
        Map<String, String> stored = fileStorageService.storeFile(file);
        String fileUrl = stored.get("fileUrl");
        String fileType = stored.get("fileType");

        if (complaintId != null) {
            Complaint complaint = complaintRepository.findById(complaintId)
                    .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));
            if (complaint.getEvidenceUrl() == null || complaint.getEvidenceUrl().isBlank()) {
                complaint.setEvidenceUrl(fileUrl);
                complaintRepository.save(complaint);
            }
            Evidence evidence = Evidence.builder()
                    .complaint(complaint)
                    .fileUrl(fileUrl)
                    .fileType(fileType)
                    .build();
            evidenceRepository.save(evidence);
        }

        return Map.of(
                "fileUrl", fileUrl,
                "fileType", fileType,
                "message", "Evidence uploaded successfully"
        );
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
                .pendingComplaints(complaintRepository.countByStatus(ComplaintStatus.PENDING.name())
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