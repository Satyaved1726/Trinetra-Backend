package com.trinetra.dto;

import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintPriority;
import com.trinetra.model.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
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

    public ComplaintResponse(
            String id,
            String title,
            String description,
            String status,
            String category,
            String priority,
            LocalDateTime createdAt
    ) {
        this.id = parseUuid(id);
        this.title = title;
        this.description = description;
        this.status = parseStatus(status);
        this.category = parseCategory(category);
        this.priority = parsePriority(priority);
        this.createdAt = createdAt;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ComplaintStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ComplaintStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ComplaintCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ComplaintCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ComplaintPriority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ComplaintPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}