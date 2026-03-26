package com.trinetra.service;

import com.trinetra.dto.AdminAnalyticsResponse;
import com.trinetra.dto.AdminAssignRequest;
import com.trinetra.dto.AdminComplaintsPageResponse;
import com.trinetra.dto.AdminNotificationResponse;
import com.trinetra.dto.AdminUserAccessResponse;
import com.trinetra.dto.AdminUsersPageResponse;
import com.trinetra.dto.AuditLogResponse;
import com.trinetra.dto.ComplaintNoteRequest;
import com.trinetra.dto.ComplaintNoteResponse;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ComplaintTimelineEventResponse;
import com.trinetra.dto.EvidenceFileResponse;
import com.trinetra.dto.UserBlockResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ComplaintNotFoundException;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.AdminUser;
import com.trinetra.model.AuditLog;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintComment;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Evidence;
import com.trinetra.model.User;
import com.trinetra.model.UserAccessControl;
import com.trinetra.repository.AdminUserRepository;
import com.trinetra.repository.AuditLogRepository;
import com.trinetra.repository.ComplaintCommentRepository;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.EvidenceRepository;
import com.trinetra.repository.UserAccessControlRepository;
import com.trinetra.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private static final Set<String> OPEN_STATUSES = Set.of(
            ComplaintStatus.SUBMITTED.name(),
            ComplaintStatus.PENDING.name(),
            ComplaintStatus.UNDER_REVIEW.name(),
            ComplaintStatus.INVESTIGATING.name()
    );

    private final ComplaintRepository complaintRepository;
    private final EvidenceRepository evidenceRepository;
    private final AdminUserRepository adminUserRepository;
    private final ComplaintCommentRepository complaintCommentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserAccessControlRepository userAccessControlRepository;

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        List<Complaint> complaints = complaintRepository.findAll();
        long total = complaints.size();
        long resolved = complaints.stream()
                .filter(c -> ComplaintStatus.RESOLVED.name().equalsIgnoreCase(c.getStatus()))
                .count();
        long open = complaints.stream()
                .filter(c -> OPEN_STATUSES.contains(c.getStatus() == null ? "" : c.getStatus().toUpperCase()))
                .count();
        long anonymous = complaints.stream().filter(c -> Boolean.TRUE.equals(c.getAnonymous())).count();

        Map<String, Long> byCategory = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));

        Map<String, Long> byStatus = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));

        Map<String, Long> overTime = complaints.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().toLocalDate().toString(),
                        java.util.TreeMap::new,
                        Collectors.counting()
                ));

        return AdminAnalyticsResponse.builder()
                .totalComplaints(total)
                .openComplaints(open)
                .resolvedComplaints(resolved)
                .anonymousComplaints(anonymous)
                .complaintsByCategory(byCategory)
                .complaintsByStatus(byStatus)
                .complaintsOverTime(overTime)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminComplaintsPageResponse getComplaints(
            String status,
            String category,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Complaint> spec = buildComplaintSpec(status, category, search, fromDate, toDate);

        Page<Complaint> complaintPage = complaintRepository.findAll(spec, pageable);
        List<Complaint> complaints = complaintPage.getContent();

        List<UUID> complaintIds = complaints.stream().map(Complaint::getId).toList();
        Map<UUID, List<Evidence>> evidenceMap = complaintIds.isEmpty()
                ? Map.of()
                : evidenceRepository.findByComplaint_IdInOrderByUploadedAtAsc(complaintIds)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getComplaint().getId()));

        List<ComplaintResponse> content = complaints.stream()
                .map(c -> toComplaintResponse(c, evidenceMap.getOrDefault(c.getId(), List.of())))
                .toList();

        return AdminComplaintsPageResponse.builder()
                .content(content)
                .page(complaintPage.getNumber())
                .size(complaintPage.getSize())
                .totalPages(complaintPage.getTotalPages())
                .totalElements(complaintPage.getTotalElements())
                .build();
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(UUID complaintId, ComplaintStatus status, String actor) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        Complaint complaint = complaintRepository.findByIdWithEvidence(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        String oldStatus = complaint.getStatus();
        complaint.setStatus(status.name());
        Complaint saved = complaintRepository.save(complaint);

        writeAudit(saved.getId(), "STATUS_UPDATED", actor, "Status changed from " + oldStatus + " to " + status.name());

        return toComplaintResponse(saved, saved.getEvidenceFiles() == null ? List.of() : saved.getEvidenceFiles());
    }

    @Transactional
    public ComplaintResponse assignComplaint(UUID complaintId, AdminAssignRequest request, String actor) {
        Complaint complaint = complaintRepository.findByIdWithEvidence(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        AdminUser assignee;
        if (request != null && request.getAdminId() != null) {
            assignee = adminUserRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        } else {
            assignee = adminUserRepository.findByUsernameIgnoreCase(actor)
                    .orElseThrow(() -> new ResourceNotFoundException("Acting admin not found"));
        }

        complaint.setAdmin(assignee);
        Complaint saved = complaintRepository.save(complaint);

        writeAudit(saved.getId(), "ASSIGNED", actor, "Complaint assigned to admin " + assignee.getUsername());

        return toComplaintResponse(saved, saved.getEvidenceFiles() == null ? List.of() : saved.getEvidenceFiles());
    }

    @Transactional(readOnly = true)
    public List<ComplaintTimelineEventResponse> getTimeline(UUID complaintId) {
        Complaint complaint = complaintRepository.findByIdWithEvidence(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        List<ComplaintTimelineEventResponse> events = new ArrayList<>();
        events.add(ComplaintTimelineEventResponse.builder()
                .eventType("CREATED")
                .description("Complaint created")
                .actor(complaint.getUserId() == null ? "ANONYMOUS" : complaint.getUserId().toString())
                .occurredAt(complaint.getCreatedAt())
                .build());

        List<Evidence> evidenceFiles = complaint.getEvidenceFiles() == null ? List.of() : complaint.getEvidenceFiles();
        for (Evidence evidence : evidenceFiles) {
            events.add(ComplaintTimelineEventResponse.builder()
                    .eventType("EVIDENCE_UPLOADED")
                    .description("Evidence uploaded: " + evidence.getFileType())
                    .actor("SYSTEM")
                    .occurredAt(evidence.getUploadedAt())
                    .build());
        }

        List<AuditLog> auditLogs = auditLogRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        for (AuditLog log : auditLogs) {
            events.add(ComplaintTimelineEventResponse.builder()
                    .eventType(log.getActionType())
                    .description(log.getActionDetails())
                    .actor(log.getActor())
                    .occurredAt(log.getCreatedAt())
                    .build());
        }

        return events.stream()
                .sorted(Comparator.comparing(ComplaintTimelineEventResponse::getOccurredAt))
                .toList();
    }

    @Transactional
    public ComplaintNoteResponse addNote(UUID complaintId, ComplaintNoteRequest request, String actor) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        AdminUser admin = adminUserRepository.findByUsernameIgnoreCase(actor)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        ComplaintComment note = ComplaintComment.builder()
                .complaint(complaint)
                .userId(admin.getId())
                .comment(request.getNote().trim())
                .build();

        ComplaintComment saved = complaintCommentRepository.save(note);
        writeAudit(complaintId, "NOTE_ADDED", actor, request.getNote().trim());

        return ComplaintNoteResponse.builder()
                .id(saved.getId())
                .complaintId(complaintId)
                .userId(saved.getUserId())
                .author(admin.getUsername())
                .note(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ComplaintNoteResponse> getNotes(UUID complaintId) {
        List<ComplaintComment> comments = complaintCommentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        List<UUID> authorIds = comments.stream().map(ComplaintComment::getUserId).distinct().toList();

        Map<UUID, String> authorMap = adminUserRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(AdminUser::getId, AdminUser::getUsername));

        return comments.stream()
                .map(note -> ComplaintNoteResponse.builder()
                        .id(note.getId())
                        .complaintId(note.getComplaint().getId())
                        .userId(note.getUserId())
                        .author(authorMap.getOrDefault(note.getUserId(), "unknown"))
                        .note(note.getComment())
                        .createdAt(note.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> getNotifications() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<AdminNotificationResponse> notifications = new ArrayList<>();

        List<Complaint> newComplaints = complaintRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(since))
                .toList();

        for (Complaint complaint : newComplaints) {
            notifications.add(AdminNotificationResponse.builder()
                    .type("NEW_COMPLAINT")
                    .complaintId(complaint.getId())
                    .message("New complaint submitted: " + complaint.getTitle())
                    .createdAt(complaint.getCreatedAt())
                    .build());
        }

        List<AuditLog> updates = auditLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since).stream()
                .limit(20)
                .toList();

        for (AuditLog update : updates) {
            notifications.add(AdminNotificationResponse.builder()
                    .type("UPDATE")
                    .complaintId(update.getComplaintId())
                    .message(update.getActionType() + ": " + update.getActionDetails())
                    .createdAt(update.getCreatedAt())
                    .build());
        }

        return notifications.stream()
                .sorted(Comparator.comparing(AdminNotificationResponse::getCreatedAt).reversed())
                .limit(40)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUsersPageResponse getUsers() {
        List<User> users = userRepository.findAll();
        List<UUID> userIds = users.stream().map(User::getId).toList();

        Map<UUID, Boolean> blockedMap = userAccessControlRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserAccessControl::getUserId, UserAccessControl::isBlocked));

        List<AdminUserAccessResponse> result = users.stream()
                .map(u -> AdminUserAccessResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .blocked(blockedMap.getOrDefault(u.getId(), false))
                        .build())
                .toList();

        return AdminUsersPageResponse.builder().users(result).build();
    }

    @Transactional
    public UserBlockResponse blockUser(UUID userId, String actor) {
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAccessControl access = userAccessControlRepository.findByUserId(userId)
                .orElseGet(() -> UserAccessControl.builder().userId(userId).build());

        access.setBlocked(true);
        access.setUpdatedBy(actor);
        UserAccessControl saved = userAccessControlRepository.save(access);

        writeAudit(null, "USER_BLOCKED", actor, "Blocked user " + userId);

        return UserBlockResponse.builder()
                .userId(saved.getUserId())
                .blocked(saved.isBlocked())
                .updatedBy(saved.getUpdatedBy())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .complaintId(log.getComplaintId())
                        .actionType(log.getActionType())
                        .actionDetails(log.getActionDetails())
                        .actor(log.getActor())
                        .createdAt(log.getCreatedAt())
                        .build());
    }

    private Specification<Complaint> buildComplaintSpec(
            String status,
            String category,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (category != null && !category.isBlank()) {
                String normalizedCategory;
                try {
                    normalizedCategory = ComplaintCategory.from(category.trim()).name();
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException(ex.getMessage());
                }
                predicates.add(cb.equal(cb.upper(root.get("category")), normalizedCategory.toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword),
                        cb.like(cb.lower(root.get("trackingId")), keyword)
                ));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ComplaintResponse toComplaintResponse(Complaint complaint, List<Evidence> evidenceList) {
        List<EvidenceFileResponse> evidenceFiles = evidenceList.stream()
                .map(e -> EvidenceFileResponse.builder()
                        .id(e.getId())
                        .fileUrl(e.getFileUrl())
                        .fileType(e.getFileType())
                        .uploadedAt(e.getUploadedAt())
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
                .adminId(complaint.getAdmin() == null ? null : complaint.getAdmin().getId())
                .evidenceFiles(evidenceFiles)
                .build();
    }

    private void writeAudit(UUID complaintId, String actionType, String actor, String details) {
        AuditLog log = AuditLog.builder()
                .complaintId(complaintId)
                .actionType(actionType)
                .actor(actor == null || actor.isBlank() ? "system" : actor)
                .actionDetails(details)
                .build();
        auditLogRepository.save(log);
    }
}
