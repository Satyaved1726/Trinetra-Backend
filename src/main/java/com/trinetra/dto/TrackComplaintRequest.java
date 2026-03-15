package com.trinetra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackComplaintRequest {

    @NotBlank(message = "trackingId is required")
    private String trackingId;

    private String anonymousToken;
}