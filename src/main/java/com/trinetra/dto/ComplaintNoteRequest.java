package com.trinetra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintNoteRequest {

    @NotBlank(message = "Note is required")
    private String note;
}
