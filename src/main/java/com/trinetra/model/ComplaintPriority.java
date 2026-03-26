package com.trinetra.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum ComplaintPriority {
    HIGH,
    MEDIUM,
    LOW;

    @JsonCreator
    public static ComplaintPriority from(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        return Arrays.stream(values())
                .filter(priority -> priority.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid complaint priority: " + value));
    }
}
