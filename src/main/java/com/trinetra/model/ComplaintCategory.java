package com.trinetra.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ComplaintCategory {
    HARASSMENT("Harassment"),
    CORRUPTION("Corruption"),
    DISCRIMINATION("Discrimination"),
    WORKPLACE_ABUSE("Workplace Abuse"),
    OTHER("Other");

    private final String label;

    ComplaintCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ComplaintCategory from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(value.replace(' ', '_'))
                        || category.label.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid complaint category: " + value));
    }
}