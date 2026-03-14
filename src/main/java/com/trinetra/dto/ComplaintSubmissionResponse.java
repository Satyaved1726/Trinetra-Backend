package com.trinetra.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComplaintSubmissionResponse {

    private String message;
    private String trackingId;
}