package com.hotel.pricing.internal.domain;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.PriceSource;
import com.hotel.pricing.api.PricedNight;
import com.hotel.pricing.api.Quote;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import com.hotel.sharedkernel.api.HotelDate;
import com.hotel.sharedkernel.api.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class QuoteCalculator {

    private QuoteCalculator() {}

    public static Quote calculate(
            UUID roomTypeId,
            RatePlan ratePlan,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            SupportedCurrency currency,
            List<PriceResolution> resolutions,
            FxLookup fxLookup,
            PricingSettings settings,
            Clock clock,
            Duration quoteTtl) {
        Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
        Objects.requireNonNull(ratePlan, "ratePlan must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(resolutions, "resolutions must not be null");
        Objects.requireNonNull(fxLookup, "fxLookup must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        long expectedNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (expectedNights != resolutions.size()) {
            throw new IllegalArgumentException("Price resolutions must cover every stay night");
        }

        LocalDate hotelToday = HotelDate.now(clock).value();
        List<PricedNight> nights = new ArrayList<>(resolutions.size());
        Money total = Money.zero(currency.name());

        for (PriceResolution resolution : resolutions) {
            Money baseAmountTry = Money.of(resolution.baseAmountTry(), "TRY");
            if (currency == SupportedCurrency.TRY) {
                Money amount = Money.of(resolution.baseAmountTry(), currency.name());
                nights.add(
                        new PricedNight(
                                resolution.date(),
                                resolution.seasonCode(),
                                resolution.seasonNameTr(),
                                resolution.seasonNameEn(),
                                resolution.priceSource(),
                                baseAmountTry,
                                null,
                                null,
                                null,
                                false,
                                null,
                                null,
                                amount));
                total = total.plus(amount);
                continue;
            }

            FxRate fxRate =
                    fxLookup
                            .find(currency, resolution.date())
                            .orElseThrow(
                                    () ->
                                            new DomainError(
                                                    PricingErrorCodes.FX_RATE_UNAVAILABLE,
                                                    "No FX rate is available for "
                                                            + currency
                                                            + " on "
                                                            + resolution.date()
                                                            + ".",
                                                    DomainErrorCategory.INTERNAL));
            BigDecimal converted =
                    FxConverter.convert(
                            resolution.baseAmountTry(),
                            fxRate.rate(),
                            settings.bufferPercent(),
                            settings.roundingStep());
            Money amount = Money.of(converted, currency.name());
            nights.add(
                    new PricedNight(
                            resolution.date(),
                            resolution.seasonCode(),
                            resolution.seasonNameTr(),
                            resolution.seasonNameEn(),
                            resolution.priceSource(),
                            baseAmountTry,
                            fxRate.rate(),
                            fxRate.source(),
                            fxRate.rateDate(),
                            fxRate.isStale(hotelToday),
                            settings.bufferPercent(),
                            settings.roundingStep(),
                            amount));
            total = total.plus(amount);
        }

        Instant quotedAt = clock.instant();
        Instant expiresAt = QuoteValidity.expiresAt(quotedAt, quoteTtl);
        return new Quote(
                roomTypeId,
                ratePlan.code(),
                ratePlan.nameTr(),
                ratePlan.nameEn(),
                checkIn,
                checkOut,
                guests,
                currency,
                nights,
                total,
                quotedAt,
                expiresAt);
    }
}
