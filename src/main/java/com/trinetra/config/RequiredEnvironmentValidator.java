package com.trinetra.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequiredEnvironmentValidator {

    private final Environment environment;

    @PostConstruct
    void validateRequiredEnvironmentVariables() {
        if (!isProductionLikeProfile()) {
            return;
        }

        List<String> missing = new ArrayList<>();
        boolean hasUrl = hasAny("DATABASE_URL", "SPRING_DATASOURCE_URL");
        if (!hasUrl) {
            missing.add("DATABASE_URL or SPRING_DATASOURCE_URL");
        }
        checkRequired("JWT_SECRET", missing);
        checkRequired("SPRING_PROFILES_ACTIVE", missing);

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required environment variables: " + String.join(", ", missing)
            );
        }
    }

    private void checkRequired(String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            missing.add(key);
        }
    }

    private boolean hasAny(String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean isProductionLikeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
