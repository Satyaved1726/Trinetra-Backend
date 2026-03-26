package com.trinetra.repository;

import com.trinetra.model.UserAccessControl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessControlRepository extends JpaRepository<UserAccessControl, UUID> {

    Optional<UserAccessControl> findByUserId(UUID userId);

    List<UserAccessControl> findByUserIdIn(List<UUID> userIds);
}
