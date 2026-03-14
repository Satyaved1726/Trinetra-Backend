package com.trinetra.dto;

import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintResponse {

    private UUID id;
    private String trackingId;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
    private boolean anonymous;
    private UUID userId;
    private UUID adminId;
}