package com.trinetra.repository;

import com.trinetra.model.ComplaintComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, UUID> {

    List<ComplaintComment> findByComplaint_IdOrderByCreatedAtAsc(UUID complaintId);
}
