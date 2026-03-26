package com.trinetra.repository;

import com.trinetra.model.Evidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByComplaint_IdOrderByUploadedAtAsc(UUID complaintId);

    List<Evidence> findByComplaint_IdInOrderByUploadedAtAsc(List<UUID> complaintIds);
}
