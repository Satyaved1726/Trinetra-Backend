package com.trinetra.repository;

import com.trinetra.model.Response;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    List<Response> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);
}