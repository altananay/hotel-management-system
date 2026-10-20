package com.hotel.catalog.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityJpaRepository extends JpaRepository<AmenityEntity, UUID> {

    Optional<AmenityEntity> findByCode(String code);

    boolean existsByCode(String code);
}
