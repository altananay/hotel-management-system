package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.internal.domain.RatePlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "pricing", name = "rate_plans")
public class RatePlanEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name_tr", nullable = false, length = 120)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPlan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RatePlanEntity() {}

    public RatePlanEntity(
            UUID id,
            String code,
            String nameTr,
            String nameEn,
            boolean active,
            boolean defaultPlan,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.nameTr = Objects.requireNonNull(nameTr);
        this.nameEn = Objects.requireNonNull(nameEn);
        this.active = active;
        this.defaultPlan = defaultPlan;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static RatePlanEntity fromDomain(RatePlan ratePlan) {
        return new RatePlanEntity(
                ratePlan.id(),
                ratePlan.code(),
                ratePlan.nameTr(),
                ratePlan.nameEn(),
                ratePlan.active(),
                ratePlan.defaultPlan(),
                ratePlan.createdAt(),
                ratePlan.updatedAt());
    }

    public RatePlan toDomain() {
        return new RatePlan(
                id, code, nameTr, nameEn, active, defaultPlan, createdAt, updatedAt);
    }

    public void update(String newNameTr, String newNameEn, boolean newActive, Instant now) {
        this.nameTr = Objects.requireNonNull(newNameTr);
        this.nameEn = Objects.requireNonNull(newNameEn);
        this.active = newActive;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void setDefaultPlan(boolean value, Instant now) {
        this.defaultPlan = value;
        this.updatedAt = Objects.requireNonNull(now);
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

    public boolean isActive() {
        return active;
    }

    public boolean isDefaultPlan() {
        return defaultPlan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
