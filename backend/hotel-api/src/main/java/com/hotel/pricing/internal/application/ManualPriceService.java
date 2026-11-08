package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.persistence.ManualPriceEntity;
import com.hotel.pricing.internal.persistence.ManualPriceJpaRepository;
import com.hotel.pricing.internal.persistence.RatePlanJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualPriceService {

    private final ManualPriceJpaRepository repository;
    private final RatePlanJpaRepository ratePlanRepository;
    private final RoomTypeLookup roomTypeLookup;
    private final Clock clock;

    public ManualPriceService(
            ManualPriceJpaRepository repository,
            RatePlanJpaRepository ratePlanRepository,
            RoomTypeLookup roomTypeLookup,
            Clock clock) {
        this.repository = repository;
        this.ratePlanRepository = ratePlanRepository;
        this.roomTypeLookup = roomTypeLookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ManualPrice> list(UUID ratePlanId) {
        if (!ratePlanRepository.existsById(ratePlanId)) {
            throw planNotFound();
        }
        return repository.findByRatePlanIdOrderByStartDateAsc(ratePlanId).stream()
                .map(ManualPriceEntity::toDomain)
                .toList();
    }

    @Transactional
    public ManualPrice create(
            UUID ratePlanId,
            UUID roomTypeId,
            int guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry) {
        ratePlanRepository.findLockedById(ratePlanId).orElseThrow(this::planNotFound);
        ActiveRoomType roomType = requireActiveRoomType(roomTypeId);
        PriceRules.requireGuests(guests);
        if (guests > roomType.maxGuests()) {
            throw invalidGuests(roomType.maxGuests());
        }
        PriceRules.requirePeriod(startDate, endDate);
        BigDecimal amount = PriceRules.requirePositiveAmount(amountTry);
        if (!repository
                .findOverlapping(ratePlanId, roomTypeId, guests, startDate, endDate)
                .isEmpty()) {
            throw overlap();
        }
        Instant now = clock.instant();
        ManualPrice manualPrice =
                ManualPrice.create(
                        PricingIds.newId(),
                        ratePlanId,
                        roomTypeId,
                        guests,
                        startDate,
                        endDate,
                        amount,
                        now);
        return repository.saveAndFlush(ManualPriceEntity.fromDomain(manualPrice)).toDomain();
    }

    @Transactional
    public ManualPrice update(
            UUID id,
            UUID roomTypeId,
            Integer guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry) {
        ManualPriceEntity entity = repository.findById(id).orElseThrow(this::notFound);
        ratePlanRepository
                .findLockedById(entity.getRatePlanId())
                .orElseThrow(this::planNotFound);
        UUID newRoomTypeId = roomTypeId == null ? entity.getRoomTypeId() : roomTypeId;
        int newGuests = guests == null ? entity.getGuests() : guests;
        LocalDate newStartDate = startDate == null ? entity.getStartDate() : startDate;
        LocalDate newEndDate = endDate == null ? entity.getEndDate() : endDate;
        BigDecimal newAmount =
                amountTry == null
                        ? entity.getAmountTry()
                        : PriceRules.requirePositiveAmount(amountTry);
        ActiveRoomType roomType = requireActiveRoomType(newRoomTypeId);
        PriceRules.requireGuests(newGuests);
        if (newGuests > roomType.maxGuests()) {
            throw invalidGuests(roomType.maxGuests());
        }
        PriceRules.requirePeriod(newStartDate, newEndDate);
        boolean overlaps =
                repository
                        .findOverlapping(
                                entity.getRatePlanId(),
                                newRoomTypeId,
                                newGuests,
                                newStartDate,
                                newEndDate)
                        .stream()
                        .anyMatch(candidate -> !candidate.getId().equals(id));
        if (overlaps) {
            throw overlap();
        }
        entity.update(
                newRoomTypeId,
                (short) newGuests,
                newStartDate,
                newEndDate,
                newAmount,
                clock.instant());
        return repository.save(entity).toDomain();
    }

    @Transactional
    public void delete(UUID id) {
        ManualPriceEntity entity = repository.findById(id).orElseThrow(this::notFound);
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public ManualPrice getRequired(UUID id) {
        return repository.findById(id).map(ManualPriceEntity::toDomain).orElseThrow(this::notFound);
    }

    private ActiveRoomType requireActiveRoomType(UUID roomTypeId) {
        return roomTypeLookup
                .findActive(RoomTypeId.of(roomTypeId))
                .orElseThrow(
                        () ->
                                new DomainError(
                                        PricingErrorCodes.ROOM_TYPE_NOT_FOUND,
                                        "The room type is not active.",
                                        DomainErrorCategory.NOT_FOUND));
    }

    private DomainError planNotFound() {
        return new DomainError(
                PricingErrorCodes.RATE_PLAN_NOT_FOUND,
                "Rate plan was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError notFound() {
        return new DomainError(
                PricingErrorCodes.MANUAL_PRICE_NOT_FOUND,
                "Manual price was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError invalidGuests(int maxGuests) {
        return new DomainError(
                PricingErrorCodes.INVALID_GUESTS,
                "Guest count must be between 1 and " + maxGuests + ".",
                DomainErrorCategory.VALIDATION);
    }

    private DomainError overlap() {
        return new DomainError(
                PricingErrorCodes.MANUAL_PRICE_OVERLAP,
                "The manual price range overlaps another manual price for the same rate plan, room type and guest count.",
                DomainErrorCategory.CONFLICT);
    }
}
