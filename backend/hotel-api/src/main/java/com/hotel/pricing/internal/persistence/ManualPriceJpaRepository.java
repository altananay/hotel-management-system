package com.hotel.pricing.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualPriceJpaRepository extends JpaRepository<ManualPriceEntity, UUID> {

    List<ManualPriceEntity> findByRatePlanIdOrderByStartDateAsc(UUID ratePlanId);

    @Query(
            "select m from ManualPriceEntity m "
                    + "where m.ratePlanId = :ratePlanId and m.roomTypeId = :roomTypeId "
                    + "and m.guests = :guests "
                    + "and m.startDate <= :endDate and m.endDate >= :startDate "
                    + "order by m.startDate asc")
    List<ManualPriceEntity> findOverlapping(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("roomTypeId") UUID roomTypeId,
            @Param("guests") int guests,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "select m from ManualPriceEntity m "
                    + "where m.ratePlanId = :ratePlanId and m.roomTypeId = :roomTypeId "
                    + "and m.guests = :guests "
                    + "and m.startDate <= :date and m.endDate >= :date")
    Optional<ManualPriceEntity> findCovering(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("roomTypeId") UUID roomTypeId,
            @Param("guests") int guests,
            @Param("date") LocalDate date);

    @Query(
            "select m from ManualPriceEntity m "
                    + "where m.ratePlanId = :ratePlanId and m.roomTypeId = :roomTypeId "
                    + "and m.guests = :guests "
                    + "and m.startDate <= :endDate and m.endDate >= :startDate "
                    + "order by m.startDate asc")
    List<ManualPriceEntity> findOverlappingForQuote(
            @Param("ratePlanId") UUID ratePlanId,
            @Param("roomTypeId") UUID roomTypeId,
            @Param("guests") int guests,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
