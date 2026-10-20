package com.hotel.catalog.internal.application;

import com.hotel.catalog.internal.domain.Amenity;
import com.hotel.catalog.internal.domain.CatalogErrorCodes;
import com.hotel.catalog.internal.domain.RoomType;
import com.hotel.catalog.internal.persistence.AmenityEntity;
import com.hotel.catalog.internal.persistence.AmenityJpaRepository;
import com.hotel.catalog.internal.persistence.RoomTypeEntity;
import com.hotel.catalog.internal.persistence.RoomTypeJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomCatalogQueryService {

    private final RoomTypeJpaRepository roomTypeRepository;
    private final AmenityJpaRepository amenityRepository;
    private final Clock clock;

    public RoomCatalogQueryService(
            RoomTypeJpaRepository roomTypeRepository,
            AmenityJpaRepository amenityRepository,
            Clock clock) {
        this.roomTypeRepository =
                Objects.requireNonNull(roomTypeRepository, "roomTypeRepository must not be null");
        this.amenityRepository =
                Objects.requireNonNull(amenityRepository, "amenityRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional(readOnly = true)
    public PagedRoomTypesResult listActiveRoomTypes(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters: page must be >= 0 and size between 1 and 100.");
        }

        long total = roomTypeRepository.countActive();
        long offset = (long) page * size;
        if (total == 0 || offset >= total) {
            return new PagedRoomTypesResult(List.of(), page, size, total);
        }

        List<RoomTypeEntity> orderedPage = roomTypeRepository.findActivePageOrdered(offset, size);
        if (orderedPage.isEmpty()) {
            return new PagedRoomTypesResult(List.of(), page, size, total);
        }

        List<UUID> orderedIds = orderedPage.stream().map(RoomTypeEntity::getId).toList();
        Map<UUID, RoomTypeEntity> hydratedById =
                roomTypeRepository.findAllWithAmenitiesByIdIn(orderedIds).stream()
                        .collect(Collectors.toMap(RoomTypeEntity::getId, Function.identity()));

        List<RoomType> items =
                orderedIds.stream()
                        .map(hydratedById::get)
                        .filter(Objects::nonNull)
                        .map(RoomTypeEntity::toDomain)
                        .toList();

        return new PagedRoomTypesResult(items, page, size, total);
    }

    @Transactional
    public Amenity createAmenity(String code, String nameTr, String nameEn) {
        Amenity amenity = Amenity.create(code, nameTr, nameEn);
        if (amenityRepository.existsByCode(amenity.code())) {
            throw DomainError.conflict(
                    CatalogErrorCodes.AMENITY_CODE_DUPLICATE,
                    "Amenity with code '" + amenity.code() + "' already exists.");
        }
        Instant now = Instant.now(clock);
        amenityRepository.save(AmenityEntity.fromDomain(amenity, now));
        return amenity;
    }

    public record PagedRoomTypesResult(List<RoomType> items, int page, int size, long total) {}
}
