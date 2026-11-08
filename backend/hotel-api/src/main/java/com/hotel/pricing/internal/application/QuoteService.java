package com.hotel.pricing.internal.application;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.pricing.api.PriceSnapshot;
import com.hotel.pricing.api.PriceSource;
import com.hotel.pricing.api.PricingQuoteApi;
import com.hotel.pricing.api.Quote;
import com.hotel.pricing.api.QuoteRequest;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxLookup;
import com.hotel.pricing.internal.domain.ManualPrice;
import com.hotel.pricing.internal.domain.PriceResolution;
import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.domain.QuoteCalculator;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.domain.RatePlanPrice;
import com.hotel.pricing.internal.domain.Season;
import com.hotel.pricing.internal.persistence.ManualPriceJpaRepository;
import com.hotel.pricing.internal.persistence.RatePlanPriceJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonPriceJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import com.hotel.sharedkernel.api.HotelDate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService implements PricingQuoteApi {

    private final RatePlanService ratePlanService;
    private final RoomTypeLookup roomTypeLookup;
    private final ManualPriceJpaRepository manualPriceRepository;
    private final SeasonJpaRepository seasonRepository;
    private final SeasonPriceJpaRepository seasonPriceRepository;
    private final RatePlanPriceJpaRepository ratePlanPriceRepository;
    private final FxRateService fxRateService;
    private final PricingSettingsService settingsService;
    private final PricingProperties properties;
    private final Clock clock;

    public QuoteService(
            RatePlanService ratePlanService,
            RoomTypeLookup roomTypeLookup,
            ManualPriceJpaRepository manualPriceRepository,
            SeasonJpaRepository seasonRepository,
            SeasonPriceJpaRepository seasonPriceRepository,
            RatePlanPriceJpaRepository ratePlanPriceRepository,
            FxRateService fxRateService,
            PricingSettingsService settingsService,
            PricingProperties properties,
            Clock clock) {
        this.ratePlanService = ratePlanService;
        this.roomTypeLookup = roomTypeLookup;
        this.manualPriceRepository = manualPriceRepository;
        this.seasonRepository = seasonRepository;
        this.seasonPriceRepository = seasonPriceRepository;
        this.ratePlanPriceRepository = ratePlanPriceRepository;
        this.fxRateService = fxRateService;
        this.settingsService = settingsService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Quote quote(QuoteRequest request) {
        if (request == null || request.roomTypeId() == null) {
            throw PriceRules.invalid("roomTypeId is required.");
        }
        if (request.checkIn() == null || request.checkOut() == null) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_PERIOD,
                    "checkIn and checkOut are required.",
                    DomainErrorCategory.VALIDATION);
        }
        LocalDate today = HotelDate.now(clock).value();
        if (request.checkIn().isBefore(today) || !request.checkOut().isAfter(request.checkIn())) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_PERIOD,
                    "checkIn must be today or later and checkOut must be after checkIn.",
                    DomainErrorCategory.VALIDATION);
        }
        long nightsCount =
                java.time.temporal.ChronoUnit.DAYS.between(
                        request.checkIn(), request.checkOut());
        if (nightsCount > properties.getMaxStayNights()) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_PERIOD,
                    "Stay must be at most " + properties.getMaxStayNights() + " nights.",
                    DomainErrorCategory.VALIDATION);
        }
        SupportedCurrency currency = SupportedCurrency.from(request.currency());
        ActiveRoomType roomType =
                roomTypeLookup
                        .findActive(RoomTypeId.of(request.roomTypeId()))
                        .orElseThrow(this::roomTypeNotFound);
        int guests = request.guests();
        PriceRules.requireGuests(guests);
        if (guests > roomType.maxGuests()) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_GUESTS,
                    "Guest count must be between 1 and " + roomType.maxGuests() + ".",
                    DomainErrorCategory.VALIDATION);
        }

        RatePlan plan = ratePlanService.findActiveDefault().orElseThrow(this::defaultPlanRequired);
        PricingSettings settings = settingsService.get();
        LocalDate lastNight = request.checkOut().minusDays(1);
        List<LocalDate> nights =
                java.util.stream.LongStream.range(0, nightsCount)
                        .mapToObj(request.checkIn()::plusDays)
                        .toList();

        List<ManualPrice> manualPrices =
                manualPriceRepository
                        .findOverlappingForQuote(
                                plan.id(),
                                request.roomTypeId(),
                                guests,
                                request.checkIn(),
                                lastNight)
                        .stream()
                        .map(entity -> entity.toDomain())
                        .toList();
        List<Season> seasons =
                seasonRepository.findOverlapping(plan.id(), request.checkIn(), lastNight).stream()
                        .map(entity -> entity.toDomain())
                        .toList();
        Map<UUID, BigDecimal> seasonPrices = seasonPrices(seasons, request.roomTypeId(), guests);
        Optional<RatePlanPrice> basePrice =
                ratePlanPriceRepository
                        .findByRatePlanIdAndRoomTypeIdAndGuests(
                                plan.id(), request.roomTypeId(), guests)
                        .map(entity -> entity.toDomain());

        List<PriceResolution> resolutions = new ArrayList<>(nights.size());
        for (LocalDate night : nights) {
            Season season =
                    seasons.stream().filter(candidate -> candidate.covers(night)).findFirst().orElse(null);
            Optional<ManualPrice> manual =
                    manualPrices.stream().filter(candidate -> candidate.covers(night)).findFirst();
            PriceSource source;
            BigDecimal amount;
            if (manual.isPresent()) {
                source = PriceSource.MANUAL;
                amount = manual.get().amountTry();
            } else if (season != null && seasonPrices.containsKey(season.id())) {
                source = PriceSource.SEASON;
                amount = seasonPrices.get(season.id());
            } else if (basePrice.isPresent()) {
                source = PriceSource.BASE;
                amount = basePrice.get().amountTry();
            } else {
                throw priceNotConfigured(request.roomTypeId(), guests, night);
            }
            resolutions.add(
                    new PriceResolution(
                            night,
                            season == null ? null : season.code(),
                            season == null ? null : season.nameTr(),
                            season == null ? null : season.nameEn(),
                            source,
                            amount));
        }

        FxLookup fxLookup =
                currency == SupportedCurrency.TRY
                        ? (requestedCurrency, date) -> Optional.empty()
                        : fxRateService.lookup(currency, request.checkIn(), lastNight);
        return QuoteCalculator.calculate(
                request.roomTypeId(),
                plan,
                request.checkIn(),
                request.checkOut(),
                guests,
                currency,
                resolutions,
                fxLookup,
                settings,
                clock,
                properties.getQuoteTtl());
    }

    @Override
    @Transactional(readOnly = true)
    public PriceSnapshot snapshot(QuoteRequest request) {
        Quote quote = quote(request);
        return new PriceSnapshot(
                quote.roomTypeId(),
                quote.ratePlanCode(),
                quote.checkIn(),
                quote.checkOut(),
                quote.guests(),
                quote.currency(),
                quote.nights(),
                quote.total(),
                quote.quotedAt());
    }

    private Map<UUID, BigDecimal> seasonPrices(
            List<Season> seasons, UUID roomTypeId, int guests) {
        if (seasons.isEmpty()) {
            return Map.of();
        }
        Collection<UUID> ids = seasons.stream().map(Season::id).toList();
        Map<UUID, BigDecimal> result = new HashMap<>();
        seasonPriceRepository
                .findBySeasonIdInAndRoomTypeIdAndGuests(ids, roomTypeId, guests)
                .forEach(entity -> result.put(entity.getSeasonId(), entity.getAmountTry()));
        return result;
    }

    private DomainError roomTypeNotFound() {
        return new DomainError(
                PricingErrorCodes.ROOM_TYPE_NOT_FOUND,
                "The room type was not found or is not active.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError defaultPlanRequired() {
        return new DomainError(
                PricingErrorCodes.DEFAULT_RATE_PLAN_REQUIRED,
                "A default active rate plan is required to quote prices.",
                DomainErrorCategory.INTERNAL);
    }

    private DomainError priceNotConfigured(UUID roomTypeId, int guests, LocalDate date) {
        return new DomainError(
                PricingErrorCodes.PRICE_NOT_CONFIGURED,
                "No price is configured for room type "
                        + roomTypeId
                        + ", "
                        + guests
                        + " guest(s) on "
                        + date
                        + ".",
                DomainErrorCategory.CONFLICT);
    }
}
