package com.trinetra.dto;

import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {

    private UUID id;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private boolean anonymous;
    private String trackingId;
    private LocalDateTime createdAt;
    private UUID userId;
    private UUID adminId;
    private List<AdminReply> responses;

    @Getter
    @Builder
    public static class AdminReply {
        private UUID id;
        private UUID adminId;
        private String adminUsername;
        private String message;
        private LocalDateTime createdAt;
    }
}