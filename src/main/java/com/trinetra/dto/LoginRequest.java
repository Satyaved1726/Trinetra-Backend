package com.trinetra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Identifier is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}