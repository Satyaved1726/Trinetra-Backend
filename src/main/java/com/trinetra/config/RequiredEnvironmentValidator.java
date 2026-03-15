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
        checkRequired("DATABASE_URL", missing);
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
