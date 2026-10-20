package com.hotel.catalog.internal.persistence;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.internal.domain.Amenity;
import com.hotel.catalog.internal.domain.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "catalog", name = "room_types")
public class RoomTypeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name_tr", nullable = false, length = 120)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "description_tr", nullable = false, length = 2000)
    private String descriptionTr;

    @Column(name = "description_en", nullable = false, length = 2000)
    private String descriptionEn;

    @Column(name = "max_guests", nullable = false)
    private short maxGuests;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "catalog",
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    @OrderBy("code ASC")
    private Set<AmenityEntity> amenities = new LinkedHashSet<>();

    protected RoomTypeEntity() {}

    public RoomTypeEntity(
            UUID id,
            String nameTr,
            String nameEn,
            String descriptionTr,
            String descriptionEn,
            short maxGuests,
            boolean active,
            int displayOrder,
            Instant createdAt,
            Instant updatedAt,
            Collection<AmenityEntity> amenities) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.nameTr = Objects.requireNonNull(nameTr, "nameTr must not be null");
        this.nameEn = Objects.requireNonNull(nameEn, "nameEn must not be null");
        this.descriptionTr = Objects.requireNonNull(descriptionTr, "descriptionTr must not be null");
        this.descriptionEn = Objects.requireNonNull(descriptionEn, "descriptionEn must not be null");
        this.maxGuests = maxGuests;
        this.active = active;
        this.displayOrder = displayOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (amenities != null) {
            this.amenities.addAll(amenities);
        }
    }

    public RoomType toDomain() {
        List<Amenity> domainAmenities =
                amenities.stream().map(AmenityEntity::toDomain).toList();
        return new RoomType(
                RoomTypeId.of(id),
                nameTr,
                nameEn,
                descriptionTr,
                descriptionEn,
                maxGuests,
                active,
                displayOrder,
                domainAmenities);
    }

    public UUID getId() {
        return id;
    }

    public String getNameTr() {
        return nameTr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescriptionTr() {
        return descriptionTr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<AmenityEntity> getAmenities() {
        return amenities;
    }
}
