package com.trinetra.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;

public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    REJECTED("Rejected");

    private final String label;

        private static final Map<ComplaintStatus, ComplaintStatus> NEXT_ALLOWED_STATUS = Map.of(
            SUBMITTED, UNDER_REVIEW,
            UNDER_REVIEW, INVESTIGATING,
            INVESTIGATING, RESOLVED,
            RESOLVED, REJECTED
        );

    ComplaintStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ComplaintStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value.replace(' ', '_'))
                        || status.label.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid complaint status: " + value));
    }

    public boolean canTransitionTo(ComplaintStatus next) {
        if (next == null || this == next) {
            return false;
        }
        return NEXT_ALLOWED_STATUS.get(this) == next;
    }
}