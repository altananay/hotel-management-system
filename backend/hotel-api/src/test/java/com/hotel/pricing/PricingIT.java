package com.hotel.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.PriceSnapshot;
import com.hotel.pricing.api.PricingDefaultIncompleteException;
import com.hotel.pricing.api.PricingQuoteApi;
import com.hotel.pricing.api.QuoteRequest;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.application.ManualFxRateService;
import com.hotel.pricing.internal.application.ManualPriceService;
import com.hotel.pricing.internal.application.PriceMatrixService;
import com.hotel.pricing.internal.application.PricingSettingsService;
import com.hotel.pricing.internal.application.RatePlanService;
import com.hotel.pricing.internal.application.SeasonService;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.persistence.FxRateEntity;
import com.hotel.pricing.internal.persistence.FxRateJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.HotelDate;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Integration tests for pricing schema, admin services and the public quote API (AC-1..AC-5). */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PricingIT {

    private static final UUID ROOM_TYPE_2 = UUID.fromString("0199f2c0-0000-7000-8000-000000000301");
    private static final UUID ROOM_TYPE_3 = UUID.fromString("0199f2c0-0000-7000-8000-000000000302");
    private static final UUID ROOM_TYPE_INACTIVE =
            UUID.fromString("0199f2c0-0000-7000-8000-000000000304");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.4-alpine")
                    .withDatabaseName("postgres")
                    .withUsername("postgres")
                    .withPassword("postgres_local_dev")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    Path.of("../../infra/postgres/init/01-init-roles-and-dbs.sql")
                                            .toAbsolutePath()
                                            .normalize()),
                            "/docker-entrypoint-initdb.d/01-init-roles-and-dbs.sql");

    private static String jdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/hotel_core",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));
    }

    @BeforeAll
    static void migrateAsMigrator() {
        Flyway.configure()
                .dataSource(jdbcUrl(), "migrator", "migrator_local_dev")
                .locations(
                        "classpath:db/migration/catalog",
                        "classpath:db/migration/pricing",
                        "classpath:db/migration/booking",
                        "classpath:db/migration/payment",
                        "classpath:db/migration/media",
                        "classpath:db/migration/notification")
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PricingIT::jdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("hotel.pricing.tcmb.enabled", () -> "false");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MockMvc mockMvc;
    @Autowired private RatePlanService ratePlanService;
    @Autowired private SeasonService seasonService;
    @Autowired private PriceMatrixService priceMatrixService;
    @Autowired private ManualPriceService manualPriceService;
    @Autowired private ManualFxRateService manualFxRateService;
    @Autowired private PricingSettingsService settingsService;
    @Autowired private PricingQuoteApi pricingQuoteApi;
    @Autowired private FxRateJpaRepository fxRateRepository;
    @Autowired private Clock clock;
    @LocalServerPort private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM pricing.manual_prices");
        jdbcTemplate.update("DELETE FROM pricing.season_prices");
        jdbcTemplate.update("DELETE FROM pricing.seasons");
        jdbcTemplate.update("DELETE FROM pricing.rate_plan_prices");
        jdbcTemplate.update("DELETE FROM pricing.rate_plans");
        jdbcTemplate.update("DELETE FROM pricing.fx_rates");
        jdbcTemplate.update(
                "UPDATE pricing.price_settings SET buffer_percent = 2.00, rounding_step = 5.00");
        jdbcTemplate.update("DELETE FROM catalog.room_type_amenities");
        jdbcTemplate.update("DELETE FROM catalog.room_types");
        insertRoomTypes();
    }

    @Test
    void shouldCreatePricingSchemaAndAllowAppDml_whenMigratedByMigrator() {
        List<String> tables =
                jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'pricing' ORDER BY table_name",
                        String.class);
        assertThat(tables)
                .contains(
                        "fx_rates",
                        "manual_prices",
                        "price_settings",
                        "rate_plan_prices",
                        "rate_plans",
                        "season_prices",
                        "seasons");
        jdbcTemplate.update(
                "INSERT INTO pricing.fx_rates "
                        + "(id, currency, source, rate_date, rate, captured_at, created_at, updated_at) "
                        + "VALUES (?, 'EUR', 'TCMB', CURRENT_DATE, 42.500000, NOW(), NOW(), NOW())",
                UUID.randomUUID());
    }

    @Test
    void shouldQuoteTryWithBaseSeasonAndManualPrice_whenPlanIsDefault() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(3);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        var season =
                seasonService.create(
                        plan.id(), "HIGH", "Yüksek Sezon", "High Season", checkIn, checkOut.minusDays(1));
        priceMatrixService.saveSeasonPrices(
                season.id(),
                List.of(new PriceMatrixService.PriceRowCommand(ROOM_TYPE_2, 2, new BigDecimal("150.00"))));
        manualPriceService.create(
                plan.id(),
                ROOM_TYPE_2,
                2,
                checkIn.plusDays(1),
                checkIn.plusDays(1),
                new BigDecimal("175.00"));

        String url =
                baseUrl()
                        + "/api/pricing/quote?roomTypeId="
                        + ROOM_TYPE_2
                        + "&checkIn="
                        + checkIn
                        + "&checkOut="
                        + checkOut
                        + "&guests=2&currency=TRY";
        JsonNode response = get(url);
        assertThat(response.get("currency").asText()).isEqualTo("TRY");
        assertThat(response.get("nights").size()).isEqualTo(3);
        assertThat(response.get("nights").get(0).get("priceSource").asText()).isEqualTo("SEASON");
        assertThat(response.get("nights").get(1).get("priceSource").asText()).isEqualTo("MANUAL");
        assertThat(response.get("nights").get(2).get("priceSource").asText()).isEqualTo("SEASON");
        assertThat(response.get("total").asText()).isEqualTo("475.00");
    }

    @Test
    void shouldQuoteEurWithBufferAndRoundEachNightUp_whenTcmbRateExists() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.01"), new BigDecimal("120.01"));
        ratePlanService.setDefault(plan.id());
        saveTcmbEurRate(HotelDate.now(clock).value().minusDays(1), new BigDecimal("32.500000"));

        String url =
                baseUrl()
                        + "/api/pricing/quote?roomTypeId="
                        + ROOM_TYPE_2
                        + "&checkIn="
                        + checkIn
                        + "&checkOut="
                        + checkOut
                        + "&guests=2&currency=EUR";
        JsonNode response = get(url);
        assertThat(response.get("currency").asText()).isEqualTo("EUR");
        assertThat(response.get("nights").get(0).get("amount").asText()).isEqualTo("3320.00");
        assertThat(response.get("total").asText()).isEqualTo("6640.00");
        assertThat(response.get("nights").get(0).get("fxSource").asText()).isEqualTo("TCMB");
        assertThat(response.get("nights").get(0).get("fxStale").asBoolean()).isTrue();
    }

    @Test
    void shouldReturn503WhenDefaultPlanIsMissingAnd409WhenPriceIsMissing() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        JsonNode noPlan =
                getRaw(
                                baseUrl()
                                        + "/api/pricing/quote?roomTypeId="
                                        + ROOM_TYPE_2
                                        + "&checkIn="
                                        + checkIn
                                        + "&checkOut="
                                        + checkOut
                                        + "&guests=2&currency=TRY")
                        .body();
        assertThat(noPlan.get("status").asInt()).isEqualTo(503);

        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        jdbcTemplate.update(
                "DELETE FROM pricing.rate_plan_prices "
                        + "WHERE rate_plan_id = ? AND room_type_id = ? AND guests = 2",
                plan.id(),
                ROOM_TYPE_2);
        JsonNode noPrice =
                getRaw(
                                baseUrl()
                                        + "/api/pricing/quote?roomTypeId="
                                        + ROOM_TYPE_2
                                        + "&checkIn="
                                        + checkIn
                                        + "&checkOut="
                                        + checkOut
                                        + "&guests=2&currency=TRY")
                        .body();
        assertThat(noPrice.get("status").asInt()).isEqualTo(409);
        assertThat(noPrice.get("code").asText()).isEqualTo(PricingErrorCodes.PRICE_NOT_CONFIGURED);
    }

    @Test
    void shouldRejectInactiveRoomTypeAndInvalidGuestsAndCurrency() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        assertThat(
                        getRaw(quoteUrl(ROOM_TYPE_INACTIVE, checkIn, checkOut, 1, "TRY"))
                                .statusCode())
                .isEqualTo(404);
        assertThat(getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkOut, 0, "TRY")).statusCode())
                .isEqualTo(400);
        assertThat(getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkOut, 3, "TRY")).statusCode())
                .isEqualTo(400);
        assertThat(getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkOut, 1, "GBP")).statusCode())
                .isEqualTo(400);
        assertThat(
                        getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkIn, 1, "TRY")).statusCode())
                .isEqualTo(400);
    }

    @Test
    void shouldRejectDefaultPlanWhenActiveRoomTypePricesAreMissing() {
        var plan = ratePlanService.create("INCOMPLETE", "Eksik", "Incomplete", true);
        priceMatrixService.saveBasePrices(
                plan.id(),
                List.of(new PriceMatrixService.PriceRowCommand(ROOM_TYPE_2, 1, new BigDecimal("100.00"))));
        assertThatThrownBy(() -> ratePlanService.setDefault(plan.id()))
                .isInstanceOf(PricingDefaultIncompleteException.class)
                .satisfies(
                        ex ->
                                assertThat(((PricingDefaultIncompleteException) ex).missing())
                                        .isNotEmpty());
    }

    private void insertRoomTypes() {
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, true, 10)",
                ROOM_TYPE_2,
                "Kemerli Mağara Süit",
                "Arched Cave Suite",
                "Açıklama",
                "Description",
                2);
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, true, 20)",
                ROOM_TYPE_3,
                "Sultan Süit",
                "Sultan Suite",
                "Açıklama",
                "Description",
                3);
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, false, 5)",
                ROOM_TYPE_INACTIVE,
                "Pasif Oda",
                "Inactive Room",
                "Açıklama",
                "Description",
                2);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturn404OverHttpWhenDeletingBasePriceThroughAnotherPlan() throws Exception {
        var planA = ratePlanService.create("PLAN-A", "Plan A", "Plan A", true);
        var planB = ratePlanService.create("PLAN-B", "Plan B", "Plan B", true);
        priceMatrixService.saveBasePrices(
                planB.id(),
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 1, new BigDecimal("100.00"))));
        UUID rowId = priceMatrixService.getBaseMatrix(planB.id()).rows().getFirst().id();

        mockMvc.perform(
                        delete(
                                "/admin-api/pricing/rate-plans/{planId}/prices/{rowId}",
                                planA.id(),
                                rowId))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code").value(PricingErrorCodes.RATE_PLAN_PRICE_NOT_FOUND));
    }

    @Test
    void shouldRejectDeletingPriceRowThroughAnotherPlanOrSeason() {
        LocalDate start = HotelDate.now(clock).value().plusDays(20);
        var planA = ratePlanService.create("PLAN-A", "Plan A", "Plan A", true);
        var planB = ratePlanService.create("PLAN-B", "Plan B", "Plan B", true);
        priceMatrixService.saveBasePrices(
                planB.id(),
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 1, new BigDecimal("100.00"))));
        UUID baseRowId =
                priceMatrixService.getBaseMatrix(planB.id()).rows().getFirst().id();

        assertThatThrownBy(() -> priceMatrixService.deleteBasePrice(planA.id(), baseRowId))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(
                                                PricingErrorCodes.RATE_PLAN_PRICE_NOT_FOUND));
        assertThat(priceMatrixService.getBaseMatrix(planB.id()).rows()).hasSize(1);

        var seasonA =
                seasonService.create(
                        planA.id(), "A-SEASON", "A Sezon", "A Season", start, start.plusDays(2));
        var seasonB =
                seasonService.create(
                        planB.id(), "B-SEASON", "B Sezon", "B Season", start, start.plusDays(2));
        priceMatrixService.saveSeasonPrices(
                seasonB.id(),
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 1, new BigDecimal("150.00"))));
        UUID seasonRowId =
                priceMatrixService.getSeasonMatrix(seasonB.id()).rows().getFirst().id();
        assertThatThrownBy(
                        () -> priceMatrixService.deleteSeasonPrice(seasonA.id(), seasonRowId))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.SEASON_PRICE_NOT_FOUND));
        assertThat(priceMatrixService.getSeasonMatrix(seasonB.id()).rows()).hasSize(1);
    }

    @Test
    void shouldDeleteBasePriceAndOnlyBreakThatCombination_whenDefaultPlanStaysDefault() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());

        UUID rowId =
                priceMatrixService.getBaseMatrix(plan.id()).rows().stream()
                        .filter(
                                row ->
                                        row.roomTypeId().equals(ROOM_TYPE_2)
                                                && row.guests() == 2)
                        .findFirst()
                        .orElseThrow()
                        .id();
        priceMatrixService.deleteBasePrice(plan.id(), rowId);

        assertThat(getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkOut, 2, "TRY")).statusCode())
                .isEqualTo(409);
        assertThat(getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkOut, 1, "TRY")).statusCode())
                .isEqualTo(200);
        assertThat(ratePlanService.findActiveDefault()).isPresent();
        assertThat(priceMatrixService.findMissingForPlan(plan.id()))
                .anySatisfy(
                        missing -> {
                            assertThat(missing.roomTypeId()).isEqualTo(ROOM_TYPE_2);
                            assertThat(missing.guests()).isEqualTo(2);
                        });
    }

    @Test
    void shouldDeleteSeasonPriceFallbackToBaseAndKeepOldSnapshotUnchanged() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        var season =
                seasonService.create(
                        plan.id(), "HIGH", "Yüksek", "High", checkIn, checkOut.minusDays(1));
        priceMatrixService.saveSeasonPrices(
                season.id(),
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 2, new BigDecimal("200.00"))));
        QuoteRequest request =
                new QuoteRequest(ROOM_TYPE_2, checkIn, checkOut, 2, "TRY");
        PriceSnapshot before = pricingQuoteApi.snapshot(request);
        assertThat(before.nights().get(0).priceSource().name()).isEqualTo("SEASON");

        UUID rowId =
                priceMatrixService.getSeasonMatrix(season.id()).rows().stream()
                        .filter(
                                row ->
                                        row.roomTypeId().equals(ROOM_TYPE_2)
                                                && row.guests() == 2)
                        .findFirst()
                        .orElseThrow()
                        .id();
        priceMatrixService.deleteSeasonPrice(season.id(), rowId);

        PriceSnapshot after = pricingQuoteApi.snapshot(request);
        assertThat(after.nights().get(0).priceSource().name()).isEqualTo("BASE");
        assertThat(before.nights().get(0).priceSource().name()).isEqualTo("SEASON");
        assertThat(before.total().amount()).isEqualByComparingTo("400.00");
        assertThat(after.total().amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldAcceptThirtyNightsAndRejectThirtyOneNights_whenQuoting() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());

        RawResponse thirty =
                getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkIn.plusDays(30), 2, "TRY"));
        assertThat(thirty.statusCode()).isEqualTo(200);
        assertThat(thirty.body().get("nightCount").asInt()).isEqualTo(30);

        RawResponse thirtyOne =
                getRaw(quoteUrl(ROOM_TYPE_2, checkIn, checkIn.plusDays(31), 2, "TRY"));
        assertThat(thirtyOne.statusCode()).isEqualTo(400);
        assertThat(thirtyOne.body().get("code").asText())
                .isEqualTo(PricingErrorCodes.INVALID_PERIOD);
    }

    @Test
    void shouldSwitchDefaultPlanBackAndForth_whenBothPlansAreComplete() {
        var planA = ratePlanService.create("PLAN-A", "Plan A", "Plan A", true);
        saveCompleteBasePrices(planA.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        var planB = ratePlanService.create("PLAN-B", "Plan B", "Plan B", true);
        saveCompleteBasePrices(planB.id(), new BigDecimal("110.00"), new BigDecimal("130.00"));

        ratePlanService.setDefault(planA.id());
        assertThat(ratePlanService.list()).anySatisfy(plan -> {
            if (plan.id().equals(planA.id())) {
                assertThat(plan.defaultPlan()).isTrue();
            }
        });
        ratePlanService.setDefault(planB.id());
        assertThat(ratePlanService.list()).anySatisfy(plan -> {
            if (plan.id().equals(planB.id())) {
                assertThat(plan.defaultPlan()).isTrue();
            }
        });
        ratePlanService.setDefault(planA.id());
        assertThat(ratePlanService.list()).anySatisfy(plan -> {
            if (plan.id().equals(planA.id())) {
                assertThat(plan.defaultPlan()).isTrue();
            }
        });
    }

    @Test
    void shouldKeepOldSnapshotUnchangedAndProduceDifferentSnapshot_whenPriceChanges() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        QuoteRequest request =
                new QuoteRequest(ROOM_TYPE_2, checkIn, checkOut, 2, "TRY");
        PriceSnapshot first = pricingQuoteApi.snapshot(request);

        priceMatrixService.saveBasePrices(
                plan.id(),
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 2, new BigDecimal("200.00"))));
        PriceSnapshot second = pricingQuoteApi.snapshot(request);

        assertThat(first.total().amount()).isEqualByComparingTo("200.00");
        assertThat(first.nights().get(0).amount().amount()).isEqualByComparingTo("100.00");
        assertThat(second.total().amount()).isEqualByComparingTo("400.00");
        assertThatThrownBy(() -> first.nights().add(first.nights().get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldPreferManualFxAndApplyUpdatedSettings_whenQuotingEur() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        saveTcmbEurRate(checkIn.minusDays(1), new BigDecimal("32.500000"));
        manualFxRateService.create("EUR", checkIn, new BigDecimal("40.000000"));

        String url =
                baseUrl()
                        + "/api/pricing/quote?roomTypeId="
                        + ROOM_TYPE_2
                        + "&checkIn="
                        + checkIn
                        + "&checkOut="
                        + checkOut
                        + "&guests=2&currency=EUR";
        JsonNode before = get(url);
        assertThat(before.get("nights").get(0).get("fxSource").asText()).isEqualTo("MANUAL");
        assertThat(before.get("nights").get(0).get("fxRate").asText()).isEqualTo("40.000000");

        settingsService.update(new BigDecimal("100.00"), new BigDecimal("10.00"));
        JsonNode after = get(url);
        assertThat(after.get("total").asText()).isNotEqualTo(before.get("total").asText());
        assertThat(after.get("nights").get(0).get("bufferPercent").asText()).isEqualTo("100.00");
        assertThat(after.get("nights").get(0).get("roundingStep").asText()).isEqualTo("10.00");
    }

    @Test
    void shouldFallbackToBasePriceAndRejectOverlapsAndDuplicates() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        var season =
                seasonService.create(
                        plan.id(), "HIGH", "Yüksek", "High", checkIn, checkIn.plusDays(5));
        assertThatThrownBy(
                        () ->
                                seasonService.create(
                                        plan.id(),
                                        "HIGH-2",
                                        "Yüksek 2",
                                        "High 2",
                                        checkIn.plusDays(1),
                                        checkIn.plusDays(6)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.SEASON_OVERLAP));
        JsonNode quote =
                get(
                        baseUrl()
                                + "/api/pricing/quote?roomTypeId="
                                + ROOM_TYPE_2
                                + "&checkIn="
                                + checkIn
                                + "&checkOut="
                                + checkOut
                                + "&guests=2&currency=TRY");
        assertThat(quote.get("nights").get(0).get("priceSource").asText()).isEqualTo("BASE");

        manualPriceService.create(
                plan.id(), ROOM_TYPE_2, 2, checkIn, checkIn, new BigDecimal("150.00"));
        assertThatThrownBy(
                        () ->
                                manualPriceService.create(
                                        plan.id(),
                                        ROOM_TYPE_2,
                                        2,
                                        checkIn,
                                        checkIn,
                                        new BigDecimal("160.00")))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.MANUAL_PRICE_OVERLAP));

        manualFxRateService.create("EUR", checkIn, new BigDecimal("40.000000"));
        assertThatThrownBy(
                        () ->
                                manualFxRateService.create(
                                        "EUR", checkIn, new BigDecimal("41.000000")))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex ->
                                assertThat(((DomainError) ex).code())
                                        .isEqualTo(PricingErrorCodes.MANUAL_FX_DUPLICATE));
    }

    @Test
    void shouldReturn503WhenEurHasNoFxRate() {
        LocalDate checkIn = HotelDate.now(clock).value().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(1);
        var plan = ratePlanService.create("STANDARD", "Standart", "Standard", true);
        saveCompleteBasePrices(plan.id(), new BigDecimal("100.00"), new BigDecimal("120.00"));
        ratePlanService.setDefault(plan.id());
        RawResponse response =
                getRaw(
                        baseUrl()
                                + "/api/pricing/quote?roomTypeId="
                                + ROOM_TYPE_2
                                + "&checkIn="
                                + checkIn
                                + "&checkOut="
                                + checkOut
                                + "&guests=2&currency=EUR");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body().get("code").asText())
                .isEqualTo(PricingErrorCodes.FX_RATE_UNAVAILABLE);
    }

    private void saveCompleteBasePrices(
            UUID ratePlanId, BigDecimal room2Guest2, BigDecimal room3Guest2) {
        priceMatrixService.saveBasePrices(
                ratePlanId,
                List.of(
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_2, 1, room2Guest2.subtract(new BigDecimal("20.00"))),
                        new PriceMatrixService.PriceRowCommand(ROOM_TYPE_2, 2, room2Guest2),
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_3, 1, room3Guest2.subtract(new BigDecimal("20.00"))),
                        new PriceMatrixService.PriceRowCommand(ROOM_TYPE_3, 2, room3Guest2),
                        new PriceMatrixService.PriceRowCommand(
                                ROOM_TYPE_3, 3, room3Guest2.add(new BigDecimal("20.00")))));
    }

    private void saveTcmbEurRate(LocalDate rateDate, BigDecimal rate) {
        Instant now = clock.instant();
        fxRateRepository.save(
                new FxRateEntity(
                        UUID.randomUUID(),
                        SupportedCurrency.EUR,
                        FxSource.TCMB,
                        rateDate,
                        rate,
                        now,
                        now,
                        now));
    }

    private String quoteUrl(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int guests, String currency) {
        return baseUrl()
                + "/api/pricing/quote?roomTypeId="
                + roomTypeId
                + "&checkIn="
                + checkIn
                + "&checkOut="
                + checkOut
                + "&guests="
                + guests
                + "&currency="
                + currency;
    }

    private JsonNode get(String url) {
        RawResponse response = getRaw(url);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private RawResponse getRaw(String url) {
        try {
            HttpResponse<String> response =
                    httpClient.send(
                            HttpRequest.newBuilder(URI.create(url)).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            return new RawResponse(response.statusCode(), objectMapper.readTree(response.body()));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private record RawResponse(int statusCode, JsonNode body) {}
}
