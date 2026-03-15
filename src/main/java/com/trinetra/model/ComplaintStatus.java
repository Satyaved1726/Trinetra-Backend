package com.trinetra.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ComplaintStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    REJECTED("Rejected");

    private final String label;

    ComplaintStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ComplaintStatus from(String value) {
        if ("submitted".equalsIgnoreCase(value)) {
            return PENDING;
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value.replace(' ', '_'))
                        || status.label.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid complaint status: " + value));
    }
}