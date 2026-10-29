package com.hotel.adminidentity.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSessionEntity, UUID> {

    Optional<AdminSessionEntity> findByTokenHash(String tokenHash);
}
