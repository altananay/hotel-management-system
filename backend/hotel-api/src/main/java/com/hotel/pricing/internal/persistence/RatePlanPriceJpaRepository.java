package com.hotel.pricing.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePlanPriceJpaRepository
        extends JpaRepository<RatePlanPriceEntity, UUID> {

    List<RatePlanPriceEntity> findByRatePlanIdOrderByRoomTypeIdAscGuestsAsc(UUID ratePlanId);

    List<RatePlanPriceEntity> findByRatePlanId(UUID ratePlanId);

    Optional<RatePlanPriceEntity> findByRatePlanIdAndRoomTypeIdAndGuests(
            UUID ratePlanId, UUID roomTypeId, int guests);
}
