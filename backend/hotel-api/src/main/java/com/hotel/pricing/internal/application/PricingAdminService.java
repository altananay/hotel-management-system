package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.Season;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PricingAdminService {

    private final RatePlanService ratePlanService;
    private final SeasonService seasonService;
    private final PriceMatrixService priceMatrixService;
    private final ManualPriceService manualPriceService;
    private final ManualFxRateService manualFxRateService;
    private final PricingSettingsService settingsService;
    private final RoomTypeLookup roomTypeLookup;

    public PricingAdminService(
            RatePlanService ratePlanService,
            SeasonService seasonService,
            PriceMatrixService priceMatrixService,
            ManualPriceService manualPriceService,
            ManualFxRateService manualFxRateService,
            PricingSettingsService settingsService,
            RoomTypeLookup roomTypeLookup) {
        this.ratePlanService = ratePlanService;
        this.seasonService = seasonService;
        this.priceMatrixService = priceMatrixService;
        this.manualPriceService = manualPriceService;
        this.manualFxRateService = manualFxRateService;
        this.settingsService = settingsService;
        this.roomTypeLookup = roomTypeLookup;
    }

    public RatePlanAdminView createRatePlan(
            String code, String nameTr, String nameEn, boolean active) {
        RatePlan plan = ratePlanService.create(code, nameTr, nameEn, active);
        return new RatePlanAdminView(plan, List.of());
    }

    public RatePlanAdminView updateRatePlan(
            UUID id, String nameTr, String nameEn, Boolean active) {
        RatePlan plan = ratePlanService.update(id, nameTr, nameEn, active);
        return new RatePlanAdminView(plan, seasonService.list(id));
    }

    public RatePlanAdminView setDefault(UUID id) {
        RatePlan plan = ratePlanService.setDefault(id);
        return new RatePlanAdminView(plan, seasonService.list(id));
    }

    public Season createSeason(
            UUID ratePlanId,
            String code,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate) {
        return seasonService.create(
                ratePlanId, code, nameTr, nameEn, startDate, endDate);
    }

    public Season updateSeason(
            UUID seasonId,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate) {
        return seasonService.update(seasonId, nameTr, nameEn, startDate, endDate);
    }

    public void deleteSeason(UUID seasonId) {
        seasonService.delete(seasonId);
    }

    public PriceMatrixService.MatrixView saveBasePrices(
            UUID ratePlanId, List<PriceMatrixService.PriceRowCommand> rows) {
        return priceMatrixService.saveBasePrices(ratePlanId, rows);
    }

    public PriceMatrixService.MatrixView saveSeasonPrices(
            UUID seasonId, List<PriceMatrixService.PriceRowCommand> rows) {
        return priceMatrixService.saveSeasonPrices(seasonId, rows);
    }

    public void deleteBasePrice(UUID ratePlanId, UUID priceRowId) {
        priceMatrixService.deleteBasePrice(ratePlanId, priceRowId);
    }

    public void deleteSeasonPrice(UUID seasonId, UUID priceRowId) {
        priceMatrixService.deleteSeasonPrice(seasonId, priceRowId);
    }

    public ManualPriceAdminView createManualPrice(
            UUID ratePlanId,
            UUID roomTypeId,
            int guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry) {
        ManualPrice manualPrice =
                manualPriceService.create(
                        ratePlanId, roomTypeId, guests, startDate, endDate, amountTry);
        return new ManualPriceAdminView(manualPrice, roomType(manualPrice.roomTypeId()));
    }

    public ManualPriceAdminView updateManualPrice(
            UUID id,
            UUID roomTypeId,
            Integer guests,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountTry) {
        ManualPrice manualPrice =
                manualPriceService.update(
                        id, roomTypeId, guests, startDate, endDate, amountTry);
        return new ManualPriceAdminView(manualPrice, roomType(manualPrice.roomTypeId()));
    }

    public void deleteManualPrice(UUID id) {
        manualPriceService.delete(id);
    }

    public FxRate createManualFxRate(
            String currency, LocalDate rateDate, BigDecimal rate) {
        return manualFxRateService.create(currency, rateDate, rate);
    }

    public FxRate updateManualFxRate(UUID id, BigDecimal rate) {
        return manualFxRateService.update(id, rate);
    }

    public void deleteManualFxRate(UUID id) {
        manualFxRateService.delete(id);
    }

    public PricingSettings updateSettings(
            BigDecimal bufferPercent, BigDecimal roundingStep) {
        return settingsService.update(bufferPercent, roundingStep);
    }

    private RoomTypeSnapshot roomType(UUID id) {
        return roomTypeLookup.findByIds(List.of(RoomTypeId.of(id))).stream()
                .findFirst()
                .orElse(null);
    }
}
