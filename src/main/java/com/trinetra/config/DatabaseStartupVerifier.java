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
            ensureComplaintStatusConstraint(connection);

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

    private void ensureComplaintStatusConstraint(Connection connection) {
        String dropConstraint = "ALTER TABLE complaints DROP CONSTRAINT IF EXISTS complaints_status_check";
        String addConstraint = """
                ALTER TABLE complaints
                ADD CONSTRAINT complaints_status_check
                CHECK (status IN ('SUBMITTED','PENDING','UNDER_REVIEW','INVESTIGATING','RESOLVED','REJECTED'))
                """;

        try (PreparedStatement drop = connection.prepareStatement(dropConstraint)) {
            drop.execute();
        } catch (SQLException ex) {
            log.warn("Unable to drop complaints_status_check: {}", ex.getMessage());
        }

        try (PreparedStatement add = connection.prepareStatement(addConstraint)) {
            add.execute();
            log.info("complaints_status_check constraint verified/updated");
        } catch (SQLException ex) {
            log.warn("Unable to add complaints_status_check: {}", ex.getMessage());
        }
    }
}