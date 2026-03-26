package com.trinetra.repository;

import com.trinetra.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime from);
}
