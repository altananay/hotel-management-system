package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxRate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "pricing", name = "fx_rates")
public class FxRateEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private SupportedCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private FxSource source;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FxRateEntity() {}

    public FxRateEntity(
            UUID id,
            SupportedCurrency currency,
            FxSource source,
            LocalDate rateDate,
            BigDecimal rate,
            Instant capturedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.currency = Objects.requireNonNull(currency);
        this.source = Objects.requireNonNull(source);
        this.rateDate = Objects.requireNonNull(rateDate);
        this.rate = Objects.requireNonNull(rate);
        this.capturedAt = Objects.requireNonNull(capturedAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static FxRateEntity fromDomain(FxRate fxRate) {
        return new FxRateEntity(
                fxRate.id(),
                fxRate.currency(),
                fxRate.source(),
                fxRate.rateDate(),
                fxRate.rate(),
                fxRate.capturedAt(),
                fxRate.createdAt(),
                fxRate.updatedAt());
    }

    public FxRate toDomain() {
        return new FxRate(
                id, currency, source, rateDate, rate, capturedAt, createdAt, updatedAt);
    }

    public void updateRate(BigDecimal newRate, Instant capturedAt, Instant now) {
        this.rate = Objects.requireNonNull(newRate);
        this.capturedAt = Objects.requireNonNull(capturedAt);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public SupportedCurrency getCurrency() {
        return currency;
    }

    public FxSource getSource() {
        return source;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
