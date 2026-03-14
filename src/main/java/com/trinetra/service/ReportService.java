package com.trinetra.service;

import com.trinetra.dto.ReportRequest;
import com.trinetra.dto.ReportResponse;
import com.trinetra.exception.BadRequestException;
import com.trinetra.exception.ResourceNotFoundException;
import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.model.Response;
import com.trinetra.repository.ComplaintRepository;
import com.trinetra.repository.ResponseRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String TRACKING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
                .category(request.getCategory())
                .status(ComplaintStatus.SUBMITTED)
                .anonymous(anonymous)
            .trackingId(generateTrackingToken())
                .userId(anonymous ? null : authenticatedUserId)
                .evidenceFiles(fileStorageService.storeFiles(files))
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
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .anonymous(complaint.isAnonymous())
                .trackingId(complaint.getTrackingId())
                .createdAt(complaint.getCreatedAt())
                .userId(complaint.getUserId())
                .evidenceFiles(complaint.getEvidenceFiles())
                .responses(responses.stream().map(response -> ReportResponse.AdminReply.builder()
                        .id(response.getId())
                        .adminId(response.getAdmin().getId())
                        .adminName(response.getAdmin().getName())
                        .message(response.getMessage())
                        .createdAt(response.getCreatedAt())
                        .build()).toList())
                .build();
    }

    private String generateTrackingToken() {
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