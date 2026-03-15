package com.trinetra.repository;

import com.trinetra.model.Complaint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByUserId(UUID userId);

    List<Complaint> findByStatus(String status);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    Optional<Complaint> findByTrackingId(String trackingId);

    Optional<Complaint> findByTrackingIdAndAnonymousToken(String trackingId, String anonymousToken);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(String status);

    long countByTrackingIdStartingWith(String prefix);
}