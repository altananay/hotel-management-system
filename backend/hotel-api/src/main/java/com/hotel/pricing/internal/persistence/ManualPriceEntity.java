package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.internal.domain.ManualPrice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "pricing", name = "manual_prices")
public class ManualPriceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rate_plan_id", nullable = false, updatable = false)
    private UUID ratePlanId;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "guests", nullable = false)
    private short guests;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "amount_try", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ManualPriceEntity() {}

    public ManualPriceEntity(
            UUID id,
            UUID ratePlanId,
            UUID roomTypeId,
            short guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.ratePlanId = Objects.requireNonNull(ratePlanId);
        this.roomTypeId = Objects.requireNonNull(roomTypeId);
        this.guests = guests;
        this.startDate = Objects.requireNonNull(startDate);
        this.endDate = Objects.requireNonNull(endDate);
        this.amountTry = Objects.requireNonNull(amountTry);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ManualPriceEntity fromDomain(ManualPrice price) {
        return new ManualPriceEntity(
                price.id(),
                price.ratePlanId(),
                price.roomTypeId(),
                (short) price.guests(),
                price.startDate(),
                price.endDate(),
                price.amountTry(),
                price.createdAt(),
                price.updatedAt());
    }

    public ManualPrice toDomain() {
        return new ManualPrice(
                id,
                ratePlanId,
                roomTypeId,
                guests,
                startDate,
                endDate,
                amountTry,
                createdAt,
                updatedAt);
    }

    public void update(
            UUID newRoomTypeId,
            short newGuests,
            LocalDate newStartDate,
            LocalDate newEndDate,
            BigDecimal newAmountTry,
            Instant now) {
        this.roomTypeId = Objects.requireNonNull(newRoomTypeId);
        this.guests = newGuests;
        this.startDate = Objects.requireNonNull(newStartDate);
        this.endDate = Objects.requireNonNull(newEndDate);
        this.amountTry = Objects.requireNonNull(newAmountTry);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRatePlanId() {
        return ratePlanId;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public int getGuests() {
        return guests;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getAmountTry() {
        return amountTry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
