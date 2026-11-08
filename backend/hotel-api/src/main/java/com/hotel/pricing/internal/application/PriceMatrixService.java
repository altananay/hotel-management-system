package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.api.MissingPrice;
import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.RatePlanPrice;
import com.hotel.pricing.internal.domain.SeasonPrice;
import com.hotel.pricing.internal.persistence.RatePlanJpaRepository;
import com.hotel.pricing.internal.persistence.RatePlanPriceEntity;
import com.hotel.pricing.internal.persistence.RatePlanPriceJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonEntity;
import com.hotel.pricing.internal.persistence.SeasonJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonPriceEntity;
import com.hotel.pricing.internal.persistence.SeasonPriceJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceMatrixService {

    private final RatePlanJpaRepository ratePlanRepository;
    private final RatePlanPriceJpaRepository ratePlanPriceRepository;
    private final SeasonJpaRepository seasonRepository;
    private final SeasonPriceJpaRepository seasonPriceRepository;
    private final RoomTypeLookup roomTypeLookup;
    private final Clock clock;

    public PriceMatrixService(
            RatePlanJpaRepository ratePlanRepository,
            RatePlanPriceJpaRepository ratePlanPriceRepository,
            SeasonJpaRepository seasonRepository,
            SeasonPriceJpaRepository seasonPriceRepository,
            RoomTypeLookup roomTypeLookup,
            Clock clock) {
        this.ratePlanRepository = ratePlanRepository;
        this.ratePlanPriceRepository = ratePlanPriceRepository;
        this.seasonRepository = seasonRepository;
        this.seasonPriceRepository = seasonPriceRepository;
        this.roomTypeLookup = roomTypeLookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MatrixView getBaseMatrix(UUID ratePlanId) {
        requirePlan(ratePlanId);
        List<RatePlanPriceEntity> rows =
                ratePlanPriceRepository.findByRatePlanIdOrderByRoomTypeIdAscGuestsAsc(ratePlanId);
        return buildMatrix(
                rows.stream().map(RatePlanPriceEntity::toDomain).toList(),
                roomTypeLookup.findAllActive(),
                false);
    }

    @Transactional
    public MatrixView saveBasePrices(UUID ratePlanId, List<PriceRowCommand> rows) {
        requireLockedPlan(ratePlanId);
        List<ValidatedPriceRow> validated = validateRows(rows);
        Instant now = clock.instant();
        for (ValidatedPriceRow row : validated) {
            RatePlanPriceEntity entity =
                    ratePlanPriceRepository
                            .findByRatePlanIdAndRoomTypeIdAndGuests(
                                    ratePlanId, row.roomTypeId(), row.guests())
                            .orElse(null);
            if (entity == null) {
                RatePlanPrice domain =
                        RatePlanPrice.create(
                                com.hotel.pricing.internal.domain.PricingIds.newId(),
                                ratePlanId,
                                row.roomTypeId(),
                                row.guests(),
                                row.amountTry(),
                                now);
                ratePlanPriceRepository.save(RatePlanPriceEntity.fromDomain(domain));
            } else {
                entity.updateAmount(row.amountTry(), now);
                ratePlanPriceRepository.save(entity);
            }
        }
        return getBaseMatrix(ratePlanId);
    }

    @Transactional(readOnly = true)
    public MatrixView getSeasonMatrix(UUID seasonId) {
        SeasonEntity season = requireSeason(seasonId);
        List<SeasonPriceEntity> rows =
                seasonPriceRepository.findBySeasonIdOrderByRoomTypeIdAscGuestsAsc(seasonId);
        return buildMatrix(
                rows.stream().map(SeasonPriceEntity::toDomain).toList(),
                roomTypeLookup.findAllActive(),
                true);
    }

    @Transactional
    public MatrixView saveSeasonPrices(UUID seasonId, List<PriceRowCommand> rows) {
        SeasonEntity season = requireSeason(seasonId);
        requireLockedPlan(season.getRatePlanId());
        List<ValidatedPriceRow> validated = validateRows(rows);
        Instant now = clock.instant();
        for (ValidatedPriceRow row : validated) {
            SeasonPriceEntity entity =
                    seasonPriceRepository
                            .findBySeasonIdAndRoomTypeIdAndGuests(
                                    seasonId, row.roomTypeId(), row.guests())
                            .orElse(null);
            if (entity == null) {
                SeasonPrice domain =
                        SeasonPrice.create(
                                com.hotel.pricing.internal.domain.PricingIds.newId(),
                                seasonId,
                                row.roomTypeId(),
                                row.guests(),
                                row.amountTry(),
                                now);
                seasonPriceRepository.save(SeasonPriceEntity.fromDomain(domain));
            } else {
                entity.updateAmount(row.amountTry(), now);
                seasonPriceRepository.save(entity);
            }
        }
        return getSeasonMatrix(seasonId);
    }

    @Transactional(readOnly = true)
    public List<MissingPrice> findMissingForPlan(UUID ratePlanId) {
        requirePlan(ratePlanId);
        List<RatePlanPriceEntity> rows = ratePlanPriceRepository.findByRatePlanId(ratePlanId);
        return missingForRows(
                roomTypeLookup.findAllActive(),
                rows.stream()
                        .map(row -> new PriceKey(row.getRoomTypeId(), row.getGuests()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public List<MissingPrice> findMissingForSeason(UUID seasonId) {
        SeasonEntity season = requireSeason(seasonId);
        List<SeasonPriceEntity> rows = seasonPriceRepository.findBySeasonIdOrderByRoomTypeIdAscGuestsAsc(seasonId);
        return missingForRows(
                roomTypeLookup.findAllActive(),
                rows.stream()
                        .map(row -> new PriceKey(row.getRoomTypeId(), row.getGuests()))
                        .toList());
    }

    private MatrixView buildMatrix(
            List<?> priceRows,
            List<ActiveRoomType> activeRoomTypes,
            boolean seasonMatrix) {
        List<RoomTypeView> roomTypes = new ArrayList<>();
        Map<UUID, RoomTypeView> byId = new LinkedHashMap<>();
        for (ActiveRoomType active : activeRoomTypes) {
            RoomTypeView view =
                    new RoomTypeView(
                            active.id().value(),
                            active.nameTr(),
                            active.nameEn(),
                            active.maxGuests(),
                            true);
            byId.put(view.id(), view);
            roomTypes.add(view);
        }

        Set<UUID> referenced = new HashSet<>();
        List<PriceRowView> rows = new ArrayList<>();
        for (Object row : priceRows) {
            UUID roomTypeId;
            int guests;
            BigDecimal amountTry;
            Instant updatedAt;
            if (row instanceof RatePlanPrice price) {
                roomTypeId = price.roomTypeId();
                guests = price.guests();
                amountTry = price.amountTry();
                updatedAt = price.updatedAt();
                rows.add(
                        new PriceRowView(
                                price.id(), roomTypeId, guests, amountTry, updatedAt));
            } else {
                SeasonPrice price = (SeasonPrice) row;
                roomTypeId = price.roomTypeId();
                guests = price.guests();
                amountTry = price.amountTry();
                updatedAt = price.updatedAt();
                rows.add(
                        new PriceRowView(
                                price.id(), roomTypeId, guests, amountTry, updatedAt));
            }
            referenced.add(roomTypeId);
        }

        List<UUID> missingRoomTypeIds =
                referenced.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missingRoomTypeIds.isEmpty()) {
            List<RoomTypeSnapshot> snapshots =
                    roomTypeLookup.findByIds(
                            missingRoomTypeIds.stream().map(RoomTypeId::of).toList());
            Map<UUID, RoomTypeSnapshot> snapshotById = new HashMap<>();
            for (RoomTypeSnapshot snapshot : snapshots) {
                snapshotById.put(snapshot.id().value(), snapshot);
            }
            for (UUID id : missingRoomTypeIds) {
                RoomTypeSnapshot snapshot = snapshotById.get(id);
                RoomTypeView view =
                        snapshot == null
                                ? new RoomTypeView(id, "Bilinmeyen oda tipi", "Unknown room type", 0, false)
                                : new RoomTypeView(
                                        id,
                                        snapshot.nameTr(),
                                        snapshot.nameEn(),
                                        snapshot.maxGuests(),
                                        snapshot.active());
                byId.put(id, view);
                roomTypes.add(view);
            }
            roomTypes.sort(
                    Comparator.comparing(RoomTypeView::active)
                            .reversed()
                            .thenComparing(RoomTypeView::nameTr));
        }

        List<PriceKey> keys =
                rows.stream().map(row -> new PriceKey(row.roomTypeId(), row.guests())).toList();
        // Season prices are optional and fall back to the plan base price; only the base
        // matrix reports missing combinations required for a default plan.
        List<MissingPrice> missing =
                seasonMatrix ? List.of() : missingForRows(activeRoomTypes, keys);
        return new MatrixView(List.copyOf(roomTypes), List.copyOf(rows), missing);
    }

    private List<MissingPrice> missingForRows(
            List<ActiveRoomType> activeRoomTypes, Collection<PriceKey> filledKeys) {
        Set<PriceKey> filled = new HashSet<>(filledKeys);
        List<MissingPrice> missing = new ArrayList<>();
        for (ActiveRoomType roomType : activeRoomTypes) {
            for (int guests = 1; guests <= roomType.maxGuests(); guests++) {
                PriceKey key = new PriceKey(roomType.id().value(), guests);
                if (!filled.contains(key)) {
                    missing.add(new MissingPrice(roomType.id().value(), guests));
                }
            }
        }
        return List.copyOf(missing);
    }

    private List<ValidatedPriceRow> validateRows(List<PriceRowCommand> rows) {
        if (rows == null || rows.isEmpty()) {
            throw PriceRules.invalid("At least one price row is required.");
        }
        Set<PriceKey> keys = new HashSet<>();
        List<ValidatedPriceRow> validated = new ArrayList<>();
        for (PriceRowCommand row : rows) {
            Objects.requireNonNull(row, "price row must not be null");
            PriceKey key = new PriceKey(row.roomTypeId(), row.guests());
            if (!keys.add(key)) {
                throw PriceRules.invalid("Duplicate room type and guest row in price matrix.");
            }
            ActiveRoomType roomType =
                    roomTypeLookup
                            .findActive(RoomTypeId.of(row.roomTypeId()))
                            .orElseThrow(this::roomTypeNotFound);
            if (row.guests() < 1 || row.guests() > roomType.maxGuests()) {
                throw new DomainError(
                        PricingErrorCodes.INVALID_GUESTS,
                        "Guest count must be between 1 and " + roomType.maxGuests() + ".",
                        DomainErrorCategory.VALIDATION);
            }
            validated.add(
                    new ValidatedPriceRow(
                            row.roomTypeId(),
                            row.guests(),
                            PriceRules.requirePositiveAmount(row.amountTry())));
        }
        return validated;
    }

    private void requirePlan(UUID ratePlanId) {
        if (!ratePlanRepository.existsById(ratePlanId)) {
            throw notFound();
        }
    }

    private void requireLockedPlan(UUID ratePlanId) {
        ratePlanRepository.findLockedById(ratePlanId).orElseThrow(this::notFound);
    }

    private SeasonEntity requireSeason(UUID seasonId) {
        return seasonRepository
                .findById(seasonId)
                .orElseThrow(
                        () ->
                                new DomainError(
                                        PricingErrorCodes.SEASON_NOT_FOUND,
                                        "Season was not found.",
                                        DomainErrorCategory.NOT_FOUND));
    }

    private DomainError notFound() {
        return new DomainError(
                PricingErrorCodes.RATE_PLAN_NOT_FOUND,
                "Rate plan was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError roomTypeNotFound() {
        return new DomainError(
                PricingErrorCodes.ROOM_TYPE_NOT_FOUND,
                "The room type is not active.",
                DomainErrorCategory.NOT_FOUND);
    }

    public record PriceRowCommand(UUID roomTypeId, int guests, BigDecimal amountTry) {}

    public record RoomTypeView(
            UUID id, String nameTr, String nameEn, int maxGuests, boolean active) {}

    public record PriceRowView(
            UUID id, UUID roomTypeId, int guests, BigDecimal amountTry, Instant updatedAt) {}

    public record MatrixView(
            List<RoomTypeView> roomTypes,
            List<PriceRowView> rows,
            List<MissingPrice> missing) {}

    private record PriceKey(UUID roomTypeId, int guests) {}

    private record ValidatedPriceRow(UUID roomTypeId, int guests, BigDecimal amountTry) {}
}
