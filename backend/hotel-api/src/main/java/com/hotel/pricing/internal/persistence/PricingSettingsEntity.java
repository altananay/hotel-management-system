package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.internal.domain.PricingSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(schema = "pricing", name = "price_settings")
public class PricingSettingsEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    @Column(name = "buffer_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal bufferPercent;

    @Column(name = "rounding_step", nullable = false, precision = 12, scale = 2)
    private BigDecimal roundingStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PricingSettingsEntity() {}

    public PricingSettingsEntity(
            Short id,
            BigDecimal bufferPercent,
            BigDecimal roundingStep,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.bufferPercent = Objects.requireNonNull(bufferPercent);
        this.roundingStep = Objects.requireNonNull(roundingStep);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public PricingSettings toDomain() {
        return new PricingSettings(bufferPercent, roundingStep, updatedAt);
    }

    public void update(BigDecimal newBufferPercent, BigDecimal newRoundingStep, Instant now) {
        this.bufferPercent = Objects.requireNonNull(newBufferPercent);
        this.roundingStep = Objects.requireNonNull(newRoundingStep);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public Short getId() {
        return id;
    }

    public BigDecimal getBufferPercent() {
        return bufferPercent;
    }

    public BigDecimal getRoundingStep() {
        return roundingStep;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
