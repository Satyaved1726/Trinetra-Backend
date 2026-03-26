package com.trinetra.dto;

import com.trinetra.model.Role;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserAccessResponse {

    private UUID id;
    private String name;
    private String email;
    private Role role;
    private boolean blocked;
}
