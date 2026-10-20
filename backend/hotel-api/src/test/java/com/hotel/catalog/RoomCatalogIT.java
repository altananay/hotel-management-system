package com.hotel.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@code catalog} V7 schema constraints (length, control-character,
 * max_guests/display_order and unique keys), active-only public listing, database-level
 * {@code ORDER BY display_order ASC, id ASC} tie-break sorting, pagination bounds and empty
 * catalog state (AC-1, AC-2, AC-5, QA-3, QA-4, QA-5, QA-6, QA-7, QA-14).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoomCatalogIT {

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

    private static String hotelCoreJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/hotel_core",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));
    }

    @BeforeAll
    static void migrateSchemaAsMigrator() {
        Flyway.configure()
                .dataSource(hotelCoreJdbcUrl(), "migrator", "migrator_local_dev")
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
        registry.add("spring.datasource.url", RoomCatalogIT::hotelCoreJdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void cleanCatalogTables() {
        jdbcTemplate.update("DELETE FROM catalog.room_type_amenities");
        jdbcTemplate.update("DELETE FROM catalog.room_types");
        jdbcTemplate.update("DELETE FROM catalog.amenities");
    }

    @Test
    void shouldReturnEmptyEnvelope_whenNoActiveRoomTypesExistInDatabase() throws Exception {
        // Insert only an inactive room type
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, false, 0)",
                UUID.fromString("0199f2c0-0000-7000-8000-000000000099"),
                "Pasif Oda",
                "Inactive Room",
                "Bakımda olan oda tipi.",
                "Room type under maintenance.",
                2);

        HttpResponse<String> response =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/room-types"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("items").isArray()).isTrue();
        assertThat(json.get("items").size()).isZero();
        assertThat(json.get("page").asInt()).isZero();
        assertThat(json.get("size").asInt()).isEqualTo(20);
        assertThat(json.get("total").asInt()).isZero();
    }

    @Test
    void shouldFilterOutInactiveAndOrderDeterministicallyByDisplayOrderThenId_whenListingRoomTypes()
            throws Exception {
        UUID amenityWifiId = UUID.fromString("0199f2c0-0000-7000-8000-000000000101");
        UUID amenityJacuzziId = UUID.fromString("0199f2c0-0000-7000-8000-000000000102");
        jdbcTemplate.update(
                "INSERT INTO catalog.amenities (id, code, name_tr, name_en) VALUES (?, ?, ?, ?)",
                amenityWifiId,
                "wifi",
                "Kablosuz İnternet",
                "Wireless Internet");
        jdbcTemplate.update(
                "INSERT INTO catalog.amenities (id, code, name_tr, name_en) VALUES (?, ?, ?, ?)",
                amenityJacuzziId,
                "cave_jacuzzi",
                "Mağara Jakuzi",
                "Cave Jacuzzi");

        // Two active room types share display_order = 10 to prove tie-breaking by id ASC (QA-5)
        UUID tieHigherId = UUID.fromString("0199f2c0-0000-7000-8000-000000000020");
        UUID tieLowerId = UUID.fromString("0199f2c0-0000-7000-8000-000000000010");
        UUID laterDisplayOrderId = UUID.fromString("0199f2c0-0000-7000-8000-000000000005");
        UUID inactiveId = UUID.fromString("0199f2c0-0000-7000-8000-000000000001");

        insertRoomType(tieHigherId, "Kemerli Süit B", "Arched Suite B", 3, true, 10);
        insertRoomType(tieLowerId, "Kemerli Süit A", "Arched Suite A", 2, true, 10);
        insertRoomType(laterDisplayOrderId, "Teras Oda", "Terrace Room", 4, true, 20);
        insertRoomType(inactiveId, "Arşiv Oda", "Archived Room", 2, false, 1);

        jdbcTemplate.update(
                "INSERT INTO catalog.room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
                tieLowerId,
                amenityWifiId);
        jdbcTemplate.update(
                "INSERT INTO catalog.room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
                tieLowerId,
                amenityJacuzziId);

        HttpResponse<String> response =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(baseUrl() + "/api/room-types?page=0&size=10"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("total").asInt()).isEqualTo(3);
        assertThat(json.get("items").size()).isEqualTo(3);

        assertThat(json.get("items").get(0).get("id").asText()).isEqualTo(tieLowerId.toString());
        assertThat(json.get("items").get(1).get("id").asText()).isEqualTo(tieHigherId.toString());
        assertThat(json.get("items").get(2).get("id").asText())
                .isEqualTo(laterDisplayOrderId.toString());

        // Amenities sorted by code ASC (cave_jacuzzi before wifi) and excluded fields absent
        JsonNode firstItem = json.get("items").get(0);
        assertThat(firstItem.get("amenities").get(0).get("code").asText())
                .isEqualTo("cave_jacuzzi");
        assertThat(firstItem.get("amenities").get(1).get("code").asText()).isEqualTo("wifi");
        assertThat(firstItem.has("active")).isFalse();
        assertThat(firstItem.has("displayOrder")).isFalse();
        assertThat(firstItem.has("createdAt")).isFalse();
        assertThat(firstItem.has("updatedAt")).isFalse();

        // Beyond last page -> 200 with empty items and total preserved (QA-4)
        HttpResponse<String> overflowResponse =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(baseUrl() + "/api/room-types?page=5&size=10"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(overflowResponse.statusCode()).isEqualTo(200);
        JsonNode overflowJson = objectMapper.readTree(overflowResponse.body());
        assertThat(overflowJson.get("items").size()).isZero();
        assertThat(overflowJson.get("total").asInt()).isEqualTo(3);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRoomTypeColumnValues")
    void shouldRejectInvalidRoomTypeColumnValues(String description, String column, Object value) {
        assertThatThrownBy(() -> insertRoomTypeWith(column, value))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAmenityColumnValues")
    void shouldRejectInvalidAmenityColumnValues(String description, String column, Object value) {
        assertThatThrownBy(() -> insertAmenityWith(column, value))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateUniqueKeys() {
        insertAmenity(
                UUID.randomUUID(),
                "wifi",
                "Kablosuz İnternet",
                "Wireless Internet");
        assertThatThrownBy(
                        () ->
                                insertAmenity(
                                        UUID.randomUUID(),
                                        "wifi",
                                        "Kablosuz İnternet 2",
                                        "Wireless Internet 2"))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID duplicateRoomTypeId = UUID.randomUUID();
        insertRoomType(duplicateRoomTypeId, "Oda", "Room", 2, true, 0);
        assertThatThrownBy(
                        () ->
                                insertRoomType(
                                        duplicateRoomTypeId, "Oda 2", "Room 2", 2, true, 0))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID duplicateAmenityId = UUID.randomUUID();
        insertAmenity(duplicateAmenityId, "spa", "Spa", "Spa");
        assertThatThrownBy(
                        () -> insertAmenity(duplicateAmenityId, "pool", "Havuz", "Pool"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateRoomTypeAmenityPair() {
        UUID roomTypeId = UUID.randomUUID();
        UUID amenityId = UUID.randomUUID();
        insertRoomType(roomTypeId, "Oda", "Room", 2, true, 0);
        insertAmenity(amenityId, "wifi", "Kablosuz İnternet", "Wireless Internet");

        jdbcTemplate.update(
                "INSERT INTO catalog.room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
                roomTypeId,
                amenityId);

        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "INSERT INTO catalog.room_type_amenities "
                                                + "(room_type_id, amenity_id) VALUES (?, ?)",
                                        roomTypeId,
                                        amenityId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<Arguments> invalidRoomTypeColumnValues() {
        return Stream.of(
                Arguments.of("name_tr length 121", "name_tr", "A".repeat(121)),
                Arguments.of("name_en length 121", "name_en", "A".repeat(121)),
                Arguments.of("description_tr length 2001", "description_tr", "A".repeat(2001)),
                Arguments.of("description_en length 2001", "description_en", "A".repeat(2001)),
                Arguments.of("name_tr whitespace only", "name_tr", "   "),
                Arguments.of("name_en whitespace only", "name_en", "   "),
                Arguments.of("description_tr whitespace only", "description_tr", "   "),
                Arguments.of("description_en whitespace only", "description_en", "   "),
                Arguments.of("name_tr null", "name_tr", null),
                Arguments.of("name_tr C0 control", "name_tr", "Oda\u0001"),
                Arguments.of("name_en C1 control", "name_en", "Room\u0085"),
                Arguments.of("name_tr DEL control", "name_tr", "Oda\u007F"),
                Arguments.of("description_tr C0 control", "description_tr", "Açıklama\u0001"),
                Arguments.of("description_en C1 control", "description_en", "Description\u0085"),
                Arguments.of("description_en DEL control", "description_en", "Description\u007F"),
                Arguments.of("name_tr angle bracket", "name_tr", "Oda <script>"),
                Arguments.of(
                        "description_en angle bracket",
                        "description_en",
                        "Description <script>"),
                Arguments.of("max_guests below lower bound", "max_guests", 0),
                Arguments.of("max_guests above upper bound", "max_guests", 21),
                Arguments.of("display_order below zero", "display_order", -1));
    }

    private static Stream<Arguments> invalidAmenityColumnValues() {
        return Stream.of(
                Arguments.of("code length 51", "code", "a".repeat(51)),
                Arguments.of("code invalid pattern", "code", "Invalid-Code"),
                Arguments.of("code too short", "code", "a"),
                Arguments.of("code blank", "code", ""),
                Arguments.of("code null", "code", null),
                Arguments.of("code C0 control", "code", "wifi\u0001"),
                Arguments.of("name_tr length 81", "name_tr", "A".repeat(81)),
                Arguments.of("name_en length 81", "name_en", "A".repeat(81)),
                Arguments.of("name_tr whitespace only", "name_tr", "   "),
                Arguments.of("name_en whitespace only", "name_en", "   "),
                Arguments.of("name_tr null", "name_tr", null),
                Arguments.of("name_en null", "name_en", null),
                Arguments.of("name_tr C0 control", "name_tr", "Olanak\u0001"),
                Arguments.of("name_en C1 control", "name_en", "Amenity\u0085"),
                Arguments.of("name_tr DEL control", "name_tr", "Olanak\u007F"),
                Arguments.of("name_tr angle bracket", "name_tr", "Olanak <script>"));
    }

    private void insertRoomTypeWith(String column, Object value) {
        Object[] args = {
            UUID.randomUUID(),
            "Oda",
            "Room",
            "Oda açıklaması",
            "Room description",
            2,
            true,
            0
        };
        switch (column) {
            case "name_tr" -> args[1] = value;
            case "name_en" -> args[2] = value;
            case "description_tr" -> args[3] = value;
            case "description_en" -> args[4] = value;
            case "max_guests" -> args[5] = value;
            case "display_order" -> args[7] = value;
            default -> throw new IllegalArgumentException("Unexpected room type column: " + column);
        }
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                args);
    }

    private void insertAmenityWith(String column, Object value) {
        Object[] args = {UUID.randomUUID(), "wifi", "Kablosuz İnternet", "Wireless Internet"};
        switch (column) {
            case "code" -> args[1] = value;
            case "name_tr" -> args[2] = value;
            case "name_en" -> args[3] = value;
            default -> throw new IllegalArgumentException("Unexpected amenity column: " + column);
        }
        jdbcTemplate.update(
                "INSERT INTO catalog.amenities (id, code, name_tr, name_en) VALUES (?, ?, ?, ?)",
                args);
    }

    private void insertAmenity(UUID id, String code, String nameTr, String nameEn) {
        jdbcTemplate.update(
                "INSERT INTO catalog.amenities (id, code, name_tr, name_en) VALUES (?, ?, ?, ?)",
                id,
                code,
                nameTr,
                nameEn);
    }

    private void insertRoomType(
            UUID id, String nameTr, String nameEn, int maxGuests, boolean active, int displayOrder) {
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                nameTr,
                nameEn,
                nameTr + " açıklama",
                nameEn + " description",
                maxGuests,
                active,
                displayOrder);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
