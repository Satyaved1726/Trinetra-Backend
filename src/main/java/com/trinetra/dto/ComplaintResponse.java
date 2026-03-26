package com.trinetra.dto;

import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintPriority;
import com.trinetra.model.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintResponse {

    private UUID id;
    private String trackingId;
    private String evidenceUrl;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean anonymous;
    private UUID userId;
    private UUID createdBy;
    private UUID adminId;
    private List<EvidenceFileResponse> evidenceFiles;
    private List<ComplaintStatusHistoryEntry> statusHistory;
    private List<ComplaintNoteResponse> notes;
}