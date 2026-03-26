package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvidenceFileResponse {

    private UUID id;
    private String fileUrl;
    private String fileType;
    private LocalDateTime uploadedAt;
}
