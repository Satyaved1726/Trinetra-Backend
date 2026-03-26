package com.trinetra.config;

import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin-patterns}") List<String> allowedOriginPatterns,
            @Value("${app.cors.frontend-origin:}") String frontendOrigin
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> originPatterns = new ArrayList<>(allowedOriginPatterns);
        if (frontendOrigin != null && !frontendOrigin.isBlank() && !originPatterns.contains(frontendOrigin.trim())) {
            originPatterns.add(frontendOrigin.trim());
        }
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}