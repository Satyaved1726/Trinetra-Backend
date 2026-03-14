package com.trinetra.dto;

import com.trinetra.model.ComplaintCategory;
import com.trinetra.model.ComplaintStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintTrackingResponse {

    private String trackingId;
    private String title;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
}