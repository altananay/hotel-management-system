package com.hotel.pricing.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.api.RoomTypeLookup;
import com.hotel.pricing.api.QuoteRequest;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.persistence.ManualPriceJpaRepository;
import com.hotel.pricing.internal.persistence.RatePlanPriceJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonPriceJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Fixed-clock boundary tests for the maximum stay rule (CR-14). */
@ExtendWith(MockitoExtension.class)
class QuoteServiceMaxStayTest {

    @Mock private RatePlanService ratePlanService;
    @Mock private RoomTypeLookup roomTypeLookup;
    @Mock private ManualPriceJpaRepository manualPriceRepository;
    @Mock private SeasonJpaRepository seasonRepository;
    @Mock private SeasonPriceJpaRepository seasonPriceRepository;
    @Mock private RatePlanPriceJpaRepository ratePlanPriceRepository;
    @Mock private FxRateService fxRateService;
    @Mock private PricingSettingsService settingsService;

    private QuoteService service;

    @BeforeEach
    void setUp() {
        Clock clock =
                Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);
        service =
                new QuoteService(
                        ratePlanService,
                        roomTypeLookup,
                        manualPriceRepository,
                        seasonRepository,
                        seasonPriceRepository,
                        ratePlanPriceRepository,
                        fxRateService,
                        settingsService,
                        new PricingProperties(),
                        clock);
    }

    @Test
    void shouldReject31NightsBeforeAnyDatabaseLookup() {
        LocalDate checkIn = LocalDate.parse("2026-11-10");
        assertThatThrownBy(
                        () ->
                                service.quote(
                                        new QuoteRequest(
                                                UUID.randomUUID(),
                                                checkIn,
                                                checkIn.plusDays(31),
                                                1,
                                                "TRY")))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.INVALID_PERIOD));

        verifyNoInteractions(roomTypeLookup, ratePlanService, fxRateService, settingsService);
    }

    @Test
    void shouldAllow30NightsToReachRoomTypeLookup() {
        LocalDate checkIn = LocalDate.parse("2026-11-10");
        UUID roomTypeId = UUID.randomUUID();
        when(roomTypeLookup.findActive(any(RoomTypeId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.quote(
                                        new QuoteRequest(
                                                roomTypeId,
                                                checkIn,
                                                checkIn.plusDays(30),
                                                1,
                                                "TRY")))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.ROOM_TYPE_NOT_FOUND));
    }
}
