package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.internal.domain.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "pricing", name = "seasons")
public class SeasonEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rate_plan_id", nullable = false, updatable = false)
    private UUID ratePlanId;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name_tr", nullable = false, length = 120)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SeasonEntity() {}

    public SeasonEntity(
            UUID id,
            UUID ratePlanId,
            String code,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.ratePlanId = Objects.requireNonNull(ratePlanId);
        this.code = Objects.requireNonNull(code);
        this.nameTr = Objects.requireNonNull(nameTr);
        this.nameEn = Objects.requireNonNull(nameEn);
        this.startDate = Objects.requireNonNull(startDate);
        this.endDate = Objects.requireNonNull(endDate);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static SeasonEntity fromDomain(Season season) {
        return new SeasonEntity(
                season.id(),
                season.ratePlanId(),
                season.code(),
                season.nameTr(),
                season.nameEn(),
                season.startDate(),
                season.endDate(),
                season.createdAt(),
                season.updatedAt());
    }

    public Season toDomain() {
        return new Season(
                id,
                ratePlanId,
                code,
                nameTr,
                nameEn,
                startDate,
                endDate,
                createdAt,
                updatedAt);
    }

    public void update(
            String newNameTr,
            String newNameEn,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Instant now) {
        this.nameTr = Objects.requireNonNull(newNameTr);
        this.nameEn = Objects.requireNonNull(newNameEn);
        this.startDate = Objects.requireNonNull(newStartDate);
        this.endDate = Objects.requireNonNull(newEndDate);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRatePlanId() {
        return ratePlanId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
