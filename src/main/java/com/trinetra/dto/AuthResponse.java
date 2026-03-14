package com.trinetra.dto;

import com.trinetra.model.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String jwtToken;
    private Role role;
    private String userId;
}