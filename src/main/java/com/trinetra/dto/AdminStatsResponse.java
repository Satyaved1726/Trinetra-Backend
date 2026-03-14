package com.trinetra.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsResponse {

    private long totalComplaints;
    private long pendingComplaints;
    private long resolvedComplaints;
    private long rejectedComplaints;
}