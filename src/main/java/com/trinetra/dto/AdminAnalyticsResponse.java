package com.trinetra.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAnalyticsResponse {

    private long totalComplaints;
    private long openComplaints;
    private long resolvedComplaints;
    private long anonymousComplaints;
    private Map<String, Long> complaintsByCategory;
    private Map<String, Long> complaintsByStatus;
    private Map<String, Long> complaintsOverTime;
}
