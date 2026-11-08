package com.hotel.pricing.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonJpaRepository extends JpaRepository<SeasonEntity, UUID> {

    List<SeasonEntity> findByRatePlanIdOrderByStartDateAsc(UUID ratePlanId);

    Optional<SeasonEntity> findByIdAndRatePlanId(UUID id, UUID ratePlanId);

    boolean existsByRatePlanIdAndCode(UUID ratePlanId, String code);

    boolean existsByRatePlanIdAndCodeAndIdNot(UUID ratePlanId, String code, UUID id);

    @Query(
            "select (count(s) > 0) from SeasonEntity s "
                    + "where s.ratePlanId = :ratePlanId "
                    + "and s.startDate <= :endDate and s.endDate >= :startDate")
    boolean existsOverlapping(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "select (count(s) > 0) from SeasonEntity s "
                    + "where s.ratePlanId = :ratePlanId and s.id <> :excludeId "
                    + "and s.startDate <= :endDate and s.endDate >= :startDate")
    boolean existsOverlappingExcluding(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("excludeId") UUID excludeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "select s from SeasonEntity s "
                    + "where s.ratePlanId = :ratePlanId "
                    + "and s.startDate <= :date and s.endDate >= :date")
    Optional<SeasonEntity> findCovering(
            @Param("ratePlanId") UUID ratePlanId, @Param("date") LocalDate date);

    @Query(
            "select s from SeasonEntity s "
                    + "where s.ratePlanId = :ratePlanId "
                    + "and s.startDate <= :endDate and s.endDate >= :startDate "
                    + "order by s.startDate asc")
    List<SeasonEntity> findOverlapping(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
