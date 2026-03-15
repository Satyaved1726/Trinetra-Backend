package com.trinetra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvidenceDTO {

    @NotBlank(message = "Evidence file URL is required")
    private String url;

    @NotBlank(message = "Evidence file type is required")
    private String type;
}
