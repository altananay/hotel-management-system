package com.hotel.catalog.internal.persistence;

import com.hotel.catalog.api.AmenityId;
import com.hotel.catalog.internal.domain.Amenity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "catalog", name = "amenities")
public class AmenityEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name_tr", nullable = false, length = 80)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AmenityEntity() {}

    public AmenityEntity(
            UUID id,
            String code,
            String nameTr,
            String nameEn,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.nameTr = Objects.requireNonNull(nameTr, "nameTr must not be null");
        this.nameEn = Objects.requireNonNull(nameEn, "nameEn must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public Amenity toDomain() {
        return new Amenity(AmenityId.of(id), code, nameTr, nameEn);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getNameTr() {
        return nameTr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
