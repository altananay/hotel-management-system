package com.hotel.catalog.internal.domain;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RoomType {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;
    public static final int MIN_MAX_GUESTS = 1;
    public static final int MAX_MAX_GUESTS = 20;

    private final RoomTypeId id;
    private final String nameTr;
    private final String nameEn;
    private final String descriptionTr;
    private final String descriptionEn;
    private final int maxGuests;
    private final boolean active;
    private final int displayOrder;
    private final List<Amenity> amenities;

    public RoomType(
            RoomTypeId id,
            String nameTr,
            String nameEn,
            String descriptionTr,
            String descriptionEn,
            int maxGuests,
            boolean active,
            int displayOrder,
            Collection<Amenity> amenities) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.nameTr =
                TextValidation.validateAndTrim(
                        nameTr,
                        MAX_NAME_LENGTH,
                        CatalogErrorCodes.ROOM_TYPE_NAME_REQUIRED,
                        CatalogErrorCodes.ROOM_TYPE_NAME_INVALID,
                        "Room type Turkish name");
        this.nameEn =
                TextValidation.validateAndTrim(
                        nameEn,
                        MAX_NAME_LENGTH,
                        CatalogErrorCodes.ROOM_TYPE_NAME_REQUIRED,
                        CatalogErrorCodes.ROOM_TYPE_NAME_INVALID,
                        "Room type English name");
        this.descriptionTr =
                TextValidation.validateAndTrim(
                        descriptionTr,
                        MAX_DESCRIPTION_LENGTH,
                        CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_REQUIRED,
                        CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_INVALID,
                        "Room type Turkish description");
        this.descriptionEn =
                TextValidation.validateAndTrim(
                        descriptionEn,
                        MAX_DESCRIPTION_LENGTH,
                        CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_REQUIRED,
                        CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_INVALID,
                        "Room type English description");
        this.maxGuests = validateMaxGuests(maxGuests);
        this.active = active;
        this.displayOrder = validateDisplayOrder(displayOrder);
        this.amenities = normalizeAndSortAmenities(amenities);
    }

    public static RoomType create(
            String nameTr,
            String nameEn,
            String descriptionTr,
            String descriptionEn,
            int maxGuests,
            boolean active,
            int displayOrder,
            Collection<Amenity> amenities) {
        return new RoomType(
                RoomTypeId.generate(),
                nameTr,
                nameEn,
                descriptionTr,
                descriptionEn,
                maxGuests,
                active,
                displayOrder,
                amenities);
    }

    public static RoomType create(
            String nameTr,
            String nameEn,
            String descriptionTr,
            String descriptionEn,
            int maxGuests,
            boolean active,
            Collection<Amenity> amenities) {
        return create(
                nameTr, nameEn, descriptionTr, descriptionEn, maxGuests, active, 0, amenities);
    }

    private static int validateMaxGuests(int maxGuests) {
        if (maxGuests < MIN_MAX_GUESTS || maxGuests > MAX_MAX_GUESTS) {
            throw new DomainError(
                    CatalogErrorCodes.ROOM_TYPE_INVALID_MAX_GUESTS,
                    "Room type maxGuests must be between 1 and 20.",
                    DomainErrorCategory.VALIDATION);
        }
        return maxGuests;
    }

    private static int validateDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new DomainError(
                    CatalogErrorCodes.ROOM_TYPE_INVALID_DISPLAY_ORDER,
                    "Room type displayOrder must be greater than or equal to 0.",
                    DomainErrorCategory.VALIDATION);
        }
        return displayOrder;
    }

    private static List<Amenity> normalizeAndSortAmenities(Collection<Amenity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return List.of();
        }
        Set<String> seenCodes = new HashSet<>();
        for (Amenity amenity : amenities) {
            Objects.requireNonNull(amenity, "amenity element must not be null");
            if (!seenCodes.add(amenity.code())) {
                throw DomainError.conflict(
                        CatalogErrorCodes.AMENITY_CODE_DUPLICATE,
                        "Duplicate amenity code in catalog: " + amenity.code());
            }
        }
        return amenities.stream()
                .sorted(Comparator.comparing(Amenity::code))
                .toList();
    }

    public RoomTypeId id() {
        return id;
    }

    public String nameTr() {
        return nameTr;
    }

    public String nameEn() {
        return nameEn;
    }

    public String descriptionTr() {
        return descriptionTr;
    }

    public String descriptionEn() {
        return descriptionEn;
    }

    public int maxGuests() {
        return maxGuests;
    }

    public boolean active() {
        return active;
    }

    public int displayOrder() {
        return displayOrder;
    }

    public List<Amenity> amenities() {
        return amenities;
    }
}
