package com.trinetra.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title must be at most 160 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @JsonAlias({"anonymous"})
    private Boolean isAnonymous;
}