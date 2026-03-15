package com.trinetra.controller;

import com.trinetra.dto.HealthStatusResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<HealthStatusResponse> health() {
        try (Connection connection = dataSource.getConnection()) {
            String dbName = connection.getCatalog() == null ? "postgres" : connection.getCatalog();
            if (connection.isValid(2)) {
                return ResponseEntity.ok(HealthStatusResponse.builder()
                        .service("TRINETRA Backend")
                        .status("RUNNING")
                        .database("CONNECTED")
                        .databaseName(dbName)
                        .build());
            }
        } catch (SQLException ex) {
            // Fall through to disconnected response.
        }
        return ResponseEntity.ok(HealthStatusResponse.builder()
                .service("TRINETRA Backend")
                .status("RUNNING")
                .database("DISCONNECTED")
                .build());
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> apiHealth() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}