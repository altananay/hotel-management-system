package com.hotel.pricing.internal.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonPriceJpaRepository extends JpaRepository<SeasonPriceEntity, UUID> {

    List<SeasonPriceEntity> findBySeasonIdOrderByRoomTypeIdAscGuestsAsc(UUID seasonId);

    Optional<SeasonPriceEntity> findBySeasonIdAndRoomTypeIdAndGuests(
            UUID seasonId, UUID roomTypeId, int guests);

    Optional<SeasonPriceEntity> findByIdAndSeasonId(UUID id, UUID seasonId);

    List<SeasonPriceEntity> findBySeasonIdInAndRoomTypeIdAndGuests(
            Collection<UUID> seasonIds, UUID roomTypeId, int guests);
}
