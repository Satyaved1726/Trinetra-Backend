package com.trinetra.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintTimelineEventResponse {

    private String eventType;
    private String description;
    private String actor;
    private LocalDateTime occurredAt;
}
