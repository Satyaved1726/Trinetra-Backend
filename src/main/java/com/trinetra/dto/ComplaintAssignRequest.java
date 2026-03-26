package com.trinetra.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintAssignRequest {

    @NotBlank(message = "assigned_to is required")
    @JsonAlias({"assignedTo"})
    private String assignedTo;
}
