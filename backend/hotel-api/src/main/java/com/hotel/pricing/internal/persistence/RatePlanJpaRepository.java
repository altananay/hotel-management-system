package com.hotel.pricing.internal.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatePlanJpaRepository extends JpaRepository<RatePlanEntity, UUID> {

    Optional<RatePlanEntity> findByCode(String code);

    Optional<RatePlanEntity> findFirstByDefaultPlanTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from RatePlanEntity p where p.id = :id")
    Optional<RatePlanEntity> findLockedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from RatePlanEntity p")
    List<RatePlanEntity> findAllLocked();

    @Query(
            "select p from RatePlanEntity p "
                    + "order by p.defaultPlan desc, p.active desc, p.code asc")
    List<RatePlanEntity> findAllOrdered();

    @Modifying
    @Query(
            "update RatePlanEntity p set p.defaultPlan = false, p.updatedAt = :now "
                    + "where p.defaultPlan = true and p.id <> :id")
    int clearOtherDefaults(@Param("id") UUID id, @Param("now") Instant now);
}
