package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID complaintId;
    private String actionType;
    private String actionDetails;
    private String actor;
    private LocalDateTime createdAt;
}
