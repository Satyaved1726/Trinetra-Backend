package com.trinetra.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAccountResponse {

    private UUID id;
    private String username;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
}
