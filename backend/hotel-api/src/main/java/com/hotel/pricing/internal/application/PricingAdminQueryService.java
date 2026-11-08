package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.web.PricingWebMapper;
import com.hotel.pricing.internal.web.dto.FxRateHistoryResponse;
import com.hotel.pricing.internal.web.dto.FxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualFxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualPriceResponse;
import com.hotel.pricing.internal.web.dto.PageResponse;
import com.hotel.pricing.internal.web.dto.PriceMatrixResponse;
import com.hotel.pricing.internal.web.dto.RatePlanResponse;
import com.hotel.pricing.internal.web.dto.RoomTypeOptionResponse;
import com.hotel.pricing.internal.web.dto.SettingsResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingAdminQueryService {

    private final RatePlanService ratePlanService;
    private final SeasonService seasonService;
    private final PriceMatrixService priceMatrixService;
    private final ManualPriceService manualPriceService;
    private final ManualFxRateService manualFxRateService;
    private final FxRateService fxRateService;
    private final PricingSettingsService settingsService;
    private final RoomTypeLookup roomTypeLookup;
    private final PricingWebMapper mapper;

    public PricingAdminQueryService(
            RatePlanService ratePlanService,
            SeasonService seasonService,
            PriceMatrixService priceMatrixService,
            ManualPriceService manualPriceService,
            ManualFxRateService manualFxRateService,
            FxRateService fxRateService,
            PricingSettingsService settingsService,
            RoomTypeLookup roomTypeLookup,
            PricingWebMapper mapper) {
        this.ratePlanService = ratePlanService;
        this.seasonService = seasonService;
        this.priceMatrixService = priceMatrixService;
        this.manualPriceService = manualPriceService;
        this.manualFxRateService = manualFxRateService;
        this.fxRateService = fxRateService;
        this.settingsService = settingsService;
        this.roomTypeLookup = roomTypeLookup;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RatePlanResponse> listRatePlans() {
        List<RatePlanResponse> result = new ArrayList<>();
        for (RatePlan plan : ratePlanService.list()) {
            result.add(mapper.toResponse(plan, seasonService.list(plan.id())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<com.hotel.pricing.internal.web.dto.SeasonResponse> listSeasons(UUID ratePlanId) {
        return seasonService.list(ratePlanId).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PriceMatrixResponse baseMatrix(UUID ratePlanId) {
        return mapper.toResponse(priceMatrixService.getBaseMatrix(ratePlanId));
    }

    @Transactional(readOnly = true)
    public PriceMatrixResponse seasonMatrix(UUID seasonId) {
        return mapper.toResponse(priceMatrixService.getSeasonMatrix(seasonId));
    }

    @Transactional(readOnly = true)
    public List<ManualPriceResponse> listManualPrices(UUID ratePlanId) {
        List<ManualPrice> prices = manualPriceService.list(ratePlanId);
        Map<UUID, RoomTypeSnapshot> roomTypes = roomTypes(prices);
        return prices.stream()
                .map(
                        price ->
                                mapper.toResponse(
                                        price,
                                        roomTypes.get(price.roomTypeId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ManualFxRateResponse> listManualFxRates(
            String currency, LocalDate from, LocalDate to, int page, int size) {
        Page<FxRate> result = manualFxRateService.list(currency, from, to, page, size);
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SettingsResponse settings() {
        PricingSettings settings = settingsService.get();
        return mapper.toResponse(settings);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeOptionResponse> activeRoomTypes() {
        return roomTypeLookup.findAllActive().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FxRateHistoryResponse fxRates(
            String currency, LocalDate from, LocalDate to, int page, int size) {
        List<FxRate> latest = new ArrayList<>();
        List<String> staleCurrencies = new ArrayList<>();
        for (SupportedCurrency supported : List.of(SupportedCurrency.EUR, SupportedCurrency.USD)) {
            FxRate tcmb = fxRateService.latestTcmb(supported).orElse(null);
            FxRate manual = fxRateService.latestManual(supported).orElse(null);
            if (tcmb != null) {
                latest.add(tcmb);
            }
            if (manual != null) {
                latest.add(manual);
            }
            if (tcmb == null || fxRateService.isStale(tcmb)) {
                staleCurrencies.add(supported.name());
            }
        }
        Page<FxRate> result = fxRateService.pageAll(currency, from, to, page, size);
        List<FxRateResponse> items = result.getContent().stream().map(mapper::toFxResponse).toList();
        List<FxRateResponse> latestResponses =
                latest.stream().map(mapper::toFxResponse).toList();
        return new FxRateHistoryResponse(
                latestResponses,
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                !staleCurrencies.isEmpty(),
                List.copyOf(staleCurrencies));
    }

    private Map<UUID, RoomTypeSnapshot> roomTypes(List<ManualPrice> prices) {
        if (prices.isEmpty()) {
            return Map.of();
        }
        Map<UUID, RoomTypeSnapshot> result = new HashMap<>();
        roomTypeLookup
                .findByIds(
                        prices.stream()
                                .map(price -> RoomTypeId.of(price.roomTypeId()))
                                .distinct()
                                .toList())
                .forEach(snapshot -> result.put(snapshot.id().value(), snapshot));
        return result;
    }
}
