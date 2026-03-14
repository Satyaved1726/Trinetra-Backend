package com.trinetra.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseStartupVerifier implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog() == null ? "postgres" : connection.getCatalog();
            String userName = connection.getMetaData().getUserName();

            Set<String> tables = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
            ); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString("table_name"));
                }
            }

            boolean hasUsers = tables.contains("users");
            boolean hasComplaints = tables.contains("complaints");

            log.info("=================================");
            log.info("TRINETRA BACKEND STARTED");
            log.info("SUPABASE DATABASE CONNECTED");
            log.info("DATABASE: {}", databaseName);
            log.info("USER: {}", userName);
            if (hasUsers && hasComplaints) {
                log.info("TRINETRA DATABASE VERIFIED");
            } else {
                log.warn("TRINETRA DATABASE WARNING - tables missing");
                log.warn("users table present: {}", hasUsers);
                log.warn("complaints table present: {}", hasComplaints);
            }
            log.info("=================================");
        } catch (SQLException ex) {
            log.error("TRINETRA DATABASE CONNECTION FAILED: {}", ex.getMessage(), ex);
        }
    }
}