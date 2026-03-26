package com.trinetra.repository;

import com.trinetra.model.Complaint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByUserId(UUID userId);

    List<Complaint> findByStatus(String status);

    @Query("SELECT DISTINCT c FROM Complaint c LEFT JOIN FETCH c.evidenceFiles ORDER BY c.createdAt DESC")
    List<Complaint> findAllWithEvidence();

    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.evidenceFiles WHERE c.id = :id")
    Optional<Complaint> findByIdWithEvidence(@Param("id") UUID id);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    Optional<Complaint> findByTrackingId(String trackingId);

    Optional<Complaint> findByTrackingIdAndAnonymousToken(String trackingId, String anonymousToken);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(String status);

    long countByTrackingIdStartingWith(String prefix);
}