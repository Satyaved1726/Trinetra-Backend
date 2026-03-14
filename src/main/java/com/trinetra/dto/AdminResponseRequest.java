package com.trinetra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminResponseRequest {

    @NotBlank(message = "Response message is required")
    @Size(max = 5000, message = "Response message must be at most 5000 characters")
    private String message;
}