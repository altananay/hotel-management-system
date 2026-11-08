package com.hotel.pricing.internal.web;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.api.PricedNight;
import com.hotel.pricing.api.Quote;
import com.hotel.pricing.internal.application.PriceMatrixService;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.Season;
import com.hotel.pricing.internal.web.dto.FxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualFxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualPriceResponse;
import com.hotel.pricing.internal.web.dto.MatrixRoomTypeResponse;
import com.hotel.pricing.internal.web.dto.MissingPriceResponse;
import com.hotel.pricing.internal.web.dto.PriceMatrixResponse;
import com.hotel.pricing.internal.web.dto.PriceRowResponse;
import com.hotel.pricing.internal.web.dto.QuoteNightResponse;
import com.hotel.pricing.internal.web.dto.QuoteResponse;
import com.hotel.pricing.internal.web.dto.RatePlanResponse;
import com.hotel.pricing.internal.web.dto.RoomTypeOptionResponse;
import com.hotel.pricing.internal.web.dto.SeasonResponse;
import com.hotel.pricing.internal.web.dto.SettingsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PricingWebMapper {

    public QuoteResponse toResponse(Quote quote) {
        return new QuoteResponse(
                quote.roomTypeId(),
                quote.ratePlanCode(),
                quote.ratePlanNameTr(),
                quote.ratePlanNameEn(),
                quote.checkIn().toString(),
                quote.checkOut().toString(),
                quote.guests(),
                quote.currency().name(),
                quote.nightCount(),
                quote.nights().stream().map(this::toResponse).toList(),
                amount(quote.total().amount()),
                instant(quote.quotedAt()),
                instant(quote.expiresAt()));
    }

    public QuoteNightResponse toResponse(PricedNight night) {
        return new QuoteNightResponse(
                night.date().toString(),
                night.seasonCode(),
                night.seasonNameTr(),
                night.seasonNameEn(),
                night.priceSource().name(),
                amount(night.baseAmountTry().amount()),
                fxRate(night.fxRate()),
                night.fxSource() == null ? null : night.fxSource().name(),
                night.fxRateDate() == null ? null : night.fxRateDate().toString(),
                night.fxRate() == null ? null : night.fxStale(),
                percent(night.bufferPercent()),
                percent(night.roundingStep()),
                amount(night.amount().amount()));
    }

    public RatePlanResponse toResponse(RatePlan plan, List<Season> seasons) {
        return new RatePlanResponse(
                plan.id(),
                plan.code(),
                plan.nameTr(),
                plan.nameEn(),
                plan.active(),
                plan.defaultPlan(),
                instant(plan.createdAt()),
                instant(plan.updatedAt()),
                seasons.stream().map(this::toResponse).toList());
    }

    public SeasonResponse toResponse(Season season) {
        return new SeasonResponse(
                season.id(),
                season.ratePlanId(),
                season.code(),
                season.nameTr(),
                season.nameEn(),
                season.startDate().toString(),
                season.endDate().toString(),
                instant(season.createdAt()),
                instant(season.updatedAt()));
    }

    public PriceMatrixResponse toResponse(PriceMatrixService.MatrixView matrix) {
        return new PriceMatrixResponse(
                matrix.roomTypes().stream()
                        .map(
                                room ->
                                        new MatrixRoomTypeResponse(
                                                room.id(),
                                                room.nameTr(),
                                                room.nameEn(),
                                                room.maxGuests(),
                                                room.active()))
                        .toList(),
                matrix.rows().stream()
                        .map(
                                row ->
                                        new PriceRowResponse(
                                                row.id(),
                                                row.roomTypeId(),
                                                row.guests(),
                                                amount(row.amountTry()),
                                                instant(row.updatedAt())))
                        .toList(),
                matrix.missing().stream()
                        .map(missing -> new MissingPriceResponse(missing.roomTypeId(), missing.guests()))
                        .toList());
    }

    public ManualPriceResponse toResponse(
            ManualPrice manualPrice, RoomTypeSnapshot roomType) {
        return new ManualPriceResponse(
                manualPrice.id(),
                manualPrice.ratePlanId(),
                manualPrice.roomTypeId(),
                roomType == null ? null : roomType.nameTr(),
                roomType == null ? null : roomType.nameEn(),
                manualPrice.guests(),
                manualPrice.startDate().toString(),
                manualPrice.endDate().toString(),
                amount(manualPrice.amountTry()),
                instant(manualPrice.createdAt()),
                instant(manualPrice.updatedAt()));
    }

    public ManualFxRateResponse toResponse(FxRate fxRate) {
        return new ManualFxRateResponse(
                fxRate.id(),
                fxRate.currency().name(),
                fxRate.source().name(),
                fxRate.rateDate().toString(),
                fxRate(fxRate.rate()),
                instant(fxRate.capturedAt()),
                instant(fxRate.updatedAt()));
    }

    public FxRateResponse toFxResponse(FxRate fxRate) {
        return new FxRateResponse(
                fxRate.id(),
                fxRate.currency().name(),
                fxRate.source().name(),
                fxRate.rateDate().toString(),
                fxRate(fxRate.rate()),
                instant(fxRate.capturedAt()),
                instant(fxRate.updatedAt()));
    }

    public SettingsResponse toResponse(PricingSettings settings) {
        return new SettingsResponse(
                percent(settings.bufferPercent()),
                percent(settings.roundingStep()),
                instant(settings.updatedAt()));
    }

    public RoomTypeOptionResponse toResponse(ActiveRoomType roomType) {
        return new RoomTypeOptionResponse(
                roomType.id().value(), roomType.nameTr(), roomType.nameEn(), roomType.maxGuests());
    }

    public static String amount(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String fxRate(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String percent(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
