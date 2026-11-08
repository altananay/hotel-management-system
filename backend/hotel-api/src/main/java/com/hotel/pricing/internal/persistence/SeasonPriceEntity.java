package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.internal.domain.SeasonPrice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "pricing", name = "season_prices")
public class SeasonPriceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "season_id", nullable = false, updatable = false)
    private UUID seasonId;

    @Column(name = "room_type_id", nullable = false, updatable = false)
    private UUID roomTypeId;

    @Column(name = "guests", nullable = false, updatable = false)
    private short guests;

    @Column(name = "amount_try", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SeasonPriceEntity() {}

    public SeasonPriceEntity(
            UUID id,
            UUID seasonId,
            UUID roomTypeId,
            short guests,
            BigDecimal amountTry,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.seasonId = Objects.requireNonNull(seasonId);
        this.roomTypeId = Objects.requireNonNull(roomTypeId);
        this.guests = guests;
        this.amountTry = Objects.requireNonNull(amountTry);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static SeasonPriceEntity fromDomain(SeasonPrice price) {
        return new SeasonPriceEntity(
                price.id(),
                price.seasonId(),
                price.roomTypeId(),
                (short) price.guests(),
                price.amountTry(),
                price.createdAt(),
                price.updatedAt());
    }

    public SeasonPrice toDomain() {
        return new SeasonPrice(
                id, seasonId, roomTypeId, guests, amountTry, createdAt, updatedAt);
    }

    public void updateAmount(BigDecimal newAmount, Instant now) {
        this.amountTry = Objects.requireNonNull(newAmount);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeasonId() {
        return seasonId;
    }

    public UUID getRoomTypeId() {
        return roomTypeId;
    }

    public int getGuests() {
        return guests;
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
