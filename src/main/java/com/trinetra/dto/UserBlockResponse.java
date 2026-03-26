package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserBlockResponse {

    private UUID userId;
    private boolean blocked;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
