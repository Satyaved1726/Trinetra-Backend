package com.trinetra.repository;

import com.trinetra.model.AdminUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<AdminUser> findFirstByRoleIgnoreCase(String role);

    List<AdminUser> findAllByRoleIgnoreCaseOrderByCreatedAtDesc(String role);
}
