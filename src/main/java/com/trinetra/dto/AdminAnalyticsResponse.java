package com.trinetra.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminAnalyticsResponse {

    private long totalComplaints;
    private long resolvedComplaints;
    private Map<String, Long> monthlyComplaints;
    private Map<String, Long> categoryStats;
}
