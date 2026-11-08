package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.Season;
import com.hotel.pricing.internal.web.PricingWebMapper;
import com.hotel.pricing.internal.web.dto.ManualFxRateCreateRequest;
import com.hotel.pricing.internal.web.dto.ManualFxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualFxRateUpdateRequest;
import com.hotel.pricing.internal.web.dto.ManualPriceCreateRequest;
import com.hotel.pricing.internal.web.dto.ManualPriceResponse;
import com.hotel.pricing.internal.web.dto.ManualPriceUpdateRequest;
import com.hotel.pricing.internal.web.dto.PriceMatrixRequest;
import com.hotel.pricing.internal.web.dto.PriceMatrixResponse;
import com.hotel.pricing.internal.web.dto.PriceRowRequest;
import com.hotel.pricing.internal.web.dto.RatePlanCreateRequest;
import com.hotel.pricing.internal.web.dto.RatePlanResponse;
import com.hotel.pricing.internal.web.dto.RatePlanUpdateRequest;
import com.hotel.pricing.internal.web.dto.SeasonCreateRequest;
import com.hotel.pricing.internal.web.dto.SeasonResponse;
import com.hotel.pricing.internal.web.dto.SeasonUpdateRequest;
import com.hotel.pricing.internal.web.dto.SettingsResponse;
import com.hotel.pricing.internal.web.dto.SettingsUpdateRequest;
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
    private final PricingWebMapper mapper;

    public PricingAdminService(
            RatePlanService ratePlanService,
            SeasonService seasonService,
            PriceMatrixService priceMatrixService,
            ManualPriceService manualPriceService,
            ManualFxRateService manualFxRateService,
            PricingSettingsService settingsService,
            RoomTypeLookup roomTypeLookup,
            PricingWebMapper mapper) {
        this.ratePlanService = ratePlanService;
        this.seasonService = seasonService;
        this.priceMatrixService = priceMatrixService;
        this.manualPriceService = manualPriceService;
        this.manualFxRateService = manualFxRateService;
        this.settingsService = settingsService;
        this.roomTypeLookup = roomTypeLookup;
        this.mapper = mapper;
    }

    public RatePlanResponse createRatePlan(RatePlanCreateRequest request) {
        RatePlan plan =
                ratePlanService.create(
                        request.code(),
                        request.nameTr(),
                        request.nameEn(),
                        request.active() == null || request.active());
        return mapper.toResponse(plan, List.of());
    }

    public RatePlanResponse updateRatePlan(UUID id, RatePlanUpdateRequest request) {
        RatePlan plan = ratePlanService.update(id, request.nameTr(), request.nameEn(), request.active());
        return mapper.toResponse(plan, seasonService.list(id));
    }

    public RatePlanResponse setDefault(UUID id) {
        RatePlan plan = ratePlanService.setDefault(id);
        return mapper.toResponse(plan, seasonService.list(id));
    }

    public SeasonResponse createSeason(UUID ratePlanId, SeasonCreateRequest request) {
        Season season =
                seasonService.create(
                        ratePlanId,
                        request.code(),
                        request.nameTr(),
                        request.nameEn(),
                        request.startDate(),
                        request.endDate());
        return mapper.toResponse(season);
    }

    public SeasonResponse updateSeason(UUID seasonId, SeasonUpdateRequest request) {
        Season season =
                seasonService.update(
                        seasonId,
                        request.nameTr(),
                        request.nameEn(),
                        request.startDate(),
                        request.endDate());
        return mapper.toResponse(season);
    }

    public void deleteSeason(UUID seasonId) {
        seasonService.delete(seasonId);
    }

    public PriceMatrixResponse saveBasePrices(UUID ratePlanId, PriceMatrixRequest request) {
        return mapper.toResponse(
                priceMatrixService.saveBasePrices(ratePlanId, commands(request.rows())));
    }

    public PriceMatrixResponse saveSeasonPrices(UUID seasonId, PriceMatrixRequest request) {
        return mapper.toResponse(
                priceMatrixService.saveSeasonPrices(seasonId, commands(request.rows())));
    }

    public ManualPriceResponse createManualPrice(ManualPriceCreateRequest request) {
        ManualPrice manualPrice =
                manualPriceService.create(
                        request.ratePlanId(),
                        request.roomTypeId(),
                        request.guests(),
                        request.startDate(),
                        request.endDate(),
                        request.amountTry());
        return mapper.toResponse(manualPrice, roomType(manualPrice.roomTypeId()));
    }

    public ManualPriceResponse updateManualPrice(
            UUID id, ManualPriceUpdateRequest request) {
        ManualPrice manualPrice =
                manualPriceService.update(
                        id,
                        request.roomTypeId(),
                        request.guests(),
                        request.startDate(),
                        request.endDate(),
                        request.amountTry());
        return mapper.toResponse(manualPrice, roomType(manualPrice.roomTypeId()));
    }

    public void deleteManualPrice(UUID id) {
        manualPriceService.delete(id);
    }

    public ManualFxRateResponse createManualFxRate(ManualFxRateCreateRequest request) {
        FxRate fxRate =
                manualFxRateService.create(request.currency(), request.rateDate(), request.rate());
        return mapper.toResponse(fxRate);
    }

    public ManualFxRateResponse updateManualFxRate(
            UUID id, ManualFxRateUpdateRequest request) {
        return mapper.toResponse(manualFxRateService.update(id, request.rate()));
    }

    public void deleteManualFxRate(UUID id) {
        manualFxRateService.delete(id);
    }

    public SettingsResponse updateSettings(SettingsUpdateRequest request) {
        PricingSettings settings =
                settingsService.update(request.bufferPercent(), request.roundingStep());
        return mapper.toResponse(settings);
    }

    private List<PriceMatrixService.PriceRowCommand> commands(List<PriceRowRequest> rows) {
        return rows.stream()
                .map(row -> new PriceMatrixService.PriceRowCommand(row.roomTypeId(), row.guests(), row.amountTry()))
                .toList();
    }

    private RoomTypeSnapshot roomType(UUID id) {
        return roomTypeLookup.findByIds(List.of(RoomTypeId.of(id))).stream().findFirst().orElse(null);
    }
}
