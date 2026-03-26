package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintNoteResponse {

    private UUID id;
    private UUID complaintId;
    private UUID userId;
    private String author;
    private String createdBy;
    private String note;
    private LocalDateTime createdAt;
}
