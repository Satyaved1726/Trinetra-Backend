package com.trinetra.service;

import com.trinetra.dto.ReportRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Response;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.ResponseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComplaintRepository complaintRepository;
    private final ResponseRepository responseRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ReportResponse submitReport(ReportRequest request, UUID authenticatedUserId, List<MultipartFile> files) {
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        if (!anonymous && authenticatedUserId == null) {
            throw new BadRequestException("Authenticated user is required for non-anonymous reports");
        }

        Complaint complaint = Complaint.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory().name())
                .status(ComplaintStatus.PENDING.name())
                .anonymous(anonymous)
                .trackingId(generateTrackingId())
                .userId(anonymous ? null : authenticatedUserId)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        return toReportResponse(savedComplaint, List.of());
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportStatus(String trackingToken) {
        Complaint complaint = complaintRepository.findByTrackingId(trackingToken)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found for the provided tracking token"));
        return toReportResponse(complaint, responseRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId()));
    }

    protected ReportResponse toReportResponse(Complaint complaint, List<Response> responses) {
        return ReportResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(ComplaintCategory.from(complaint.getCategory()))
                .status(ComplaintStatus.from(complaint.getStatus()))
                .anonymous(Boolean.TRUE.equals(complaint.getAnonymous()))
                .trackingId(complaint.getTrackingId())
                .createdAt(complaint.getCreatedAt())
                .userId(complaint.getUserId())
                .adminId(complaint.getAdmin() != null ? complaint.getAdmin().getId() : null)
                .responses(responses.stream().map(response -> ReportResponse.AdminReply.builder()
                        .id(response.getId())
                        .adminId(response.getAdmin().getId())
                        .adminUsername(response.getAdmin().getUsername())
                        .message(response.getMessage())
                        .createdAt(response.getCreatedAt())
                        .build()).toList())
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
}