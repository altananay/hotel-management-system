package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.catalog.api.RoomTypeSnapshot;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.Season;
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

    public PricingAdminQueryService(
            RatePlanService ratePlanService,
            SeasonService seasonService,
            PriceMatrixService priceMatrixService,
            ManualPriceService manualPriceService,
            ManualFxRateService manualFxRateService,
            FxRateService fxRateService,
            PricingSettingsService settingsService,
            RoomTypeLookup roomTypeLookup) {
        this.ratePlanService = ratePlanService;
        this.seasonService = seasonService;
        this.priceMatrixService = priceMatrixService;
        this.manualPriceService = manualPriceService;
        this.manualFxRateService = manualFxRateService;
        this.fxRateService = fxRateService;
        this.settingsService = settingsService;
        this.roomTypeLookup = roomTypeLookup;
    }

    @Transactional(readOnly = true)
    public List<RatePlanAdminView> listRatePlans() {
        List<RatePlanAdminView> result = new ArrayList<>();
        for (RatePlan plan : ratePlanService.list()) {
            result.add(new RatePlanAdminView(plan, seasonService.list(plan.id())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Season> listSeasons(UUID ratePlanId) {
        return seasonService.list(ratePlanId);
    }

    @Transactional(readOnly = true)
    public PriceMatrixService.MatrixView baseMatrix(UUID ratePlanId) {
        return priceMatrixService.getBaseMatrix(ratePlanId);
    }

    @Transactional(readOnly = true)
    public PriceMatrixService.MatrixView seasonMatrix(UUID seasonId) {
        return priceMatrixService.getSeasonMatrix(seasonId);
    }

    @Transactional(readOnly = true)
    public List<ManualPriceAdminView> listManualPrices(UUID ratePlanId) {
        List<ManualPrice> prices = manualPriceService.list(ratePlanId);
        Map<UUID, RoomTypeSnapshot> roomTypes = roomTypes(prices);
        return prices.stream()
                .map(price -> new ManualPriceAdminView(price, roomTypes.get(price.roomTypeId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<FxRate> listManualFxRates(
            String currency, LocalDate from, LocalDate to, int page, int size) {
        return manualFxRateService.list(currency, from, to, page, size);
    }

    @Transactional(readOnly = true)
    public PricingSettings settings() {
        return settingsService.get();
    }

    @Transactional(readOnly = true)
    public List<ActiveRoomType> activeRoomTypes() {
        return roomTypeLookup.findAllActive();
    }

    @Transactional(readOnly = true)
    public FxRateHistoryView fxRates(
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
        return new FxRateHistoryView(
                latest,
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                !staleCurrencies.isEmpty(),
                staleCurrencies);
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
