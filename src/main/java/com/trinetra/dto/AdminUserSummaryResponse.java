package com.trinetra.dto;

import com.trinetra.model.Role;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserSummaryResponse {

    private UUID id;
    private String name;
    private String email;
    private Role role;
}
