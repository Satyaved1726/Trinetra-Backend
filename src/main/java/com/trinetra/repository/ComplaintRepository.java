package com.trinetra.repository;

import com.trinetra.model.Complaint;
import com.trinetra.model.ComplaintStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByUserId(UUID userId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    Optional<Complaint> findByTrackingId(String trackingId);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(ComplaintStatus status);
}