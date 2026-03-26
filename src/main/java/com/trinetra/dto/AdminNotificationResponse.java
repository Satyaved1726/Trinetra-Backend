package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminNotificationResponse {

    private String type;
    private UUID complaintId;
    private String message;
    private LocalDateTime createdAt;
}
