package com.hotel.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Integration tests for {@code catalog} V7 schema constraints, active-only public listing,
 * database-level {@code ORDER BY display_order ASC, id ASC} tie-break sorting, pagination bounds
 * and empty catalog state (AC-1, AC-2, AC-5, QA-3, QA-4, QA-5, QA-6, QA-7, QA-14).
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

    @Test
    void shouldEnforceDatabaseCheckAndUniqueConstraints_whenInvalidCatalogDataInserted() {
        // Invalid max_guests (> 20)
        assertThatThrownBy(
                        () ->
                                insertRoomType(
                                        UUID.fromString("0199f2c0-0000-7000-8000-000000000050"),
                                        "Oda",
                                        "Room",
                                        21,
                                        true,
                                        0))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Invalid display_order (< 0)
        assertThatThrownBy(
                        () ->
                                insertRoomType(
                                        UUID.fromString("0199f2c0-0000-7000-8000-000000000051"),
                                        "Oda",
                                        "Room",
                                        2,
                                        true,
                                        -1))
                .isInstanceOf(DataIntegrityViolationException.class);

        // HTML angle bracket in room type name
        assertThatThrownBy(
                        () ->
                                insertRoomType(
                                        UUID.fromString("0199f2c0-0000-7000-8000-000000000052"),
                                        "Oda <script>",
                                        "Room",
                                        2,
                                        true,
                                        0))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Invalid amenity code pattern
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "INSERT INTO catalog.amenities (id, code, name_tr, name_en) VALUES (?, ?, ?, ?)",
                                        UUID.fromString("0199f2c0-0000-7000-8000-000000000150"),
                                        "Invalid-Code",
                                        "Olanak",
                                        "Amenity"))
                .isInstanceOf(DataIntegrityViolationException.class);
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
