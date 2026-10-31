package com.hotel.adminidentity.internal.persistence;

import com.hotel.adminidentity.internal.domain.AdminRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, UUID> {

    Optional<AdminUserEntity> findByUsernameIgnoreCase(String username);

    Optional<AdminUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByRoleAndActiveTrue(AdminRole role);

    List<AdminUserEntity> findAllByRoleAndActiveTrue(AdminRole role);
}
