package com.hotel.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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
 * Integration test verifying that standard migrations create an empty catalog and that executing
 * {@code db/seed/catalog/R__seed_catalog_dev.sql} directly twice as {@code migrator} is idempotent
 * (3 active + 1 inactive room types, 5 amenities, tie-break order on {@code display_order = 10},
 * and {@code GET /api/room-types} returning 3 items) and that seed fails when the local/CI
 * database comment marker is absent (AC-5, QA-1, QA-9, QA-17, QA-18).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogSeedIT {

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
    static void migrateStandardSchemaWithoutSeed() {
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
        registry.add("spring.datasource.url", CatalogSeedIT::hotelCoreJdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldStartEmptyAfterStandardMigrateAndRemainIdempotentWhenSeedExecutedTwiceAsMigrator()
            throws Exception {
        // 1. Standard migration without seed leaves catalog tables empty
        Integer initialRoomTypes =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.room_types", Integer.class);
        Integer initialAmenities =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.amenities", Integer.class);
        assertThat(initialRoomTypes).isZero();
        assertThat(initialAmenities).isZero();

        // 2. Execute seed SQL directly as migrator — Run #1 (QA-17)
        executeSeedScriptDirectlyAsMigrator();
        assertSeededCountsAndPublicApiResponse();

        // 3. Execute seed SQL directly a second time as migrator — Run #2 (proves true SQL idempotency)
        executeSeedScriptDirectlyAsMigrator();
        assertSeededCountsAndPublicApiResponse();
    }

    private void executeSeedScriptDirectlyAsMigrator() throws Exception {
        try (Connection connection =
                DriverManager.getConnection(
                        hotelCoreJdbcUrl(), "migrator", "migrator_local_dev")) {
            ScriptUtils.executeSqlScript(
                    connection, new ClassPathResource("db/seed/catalog/R__seed_catalog_dev.sql"));
        }
    }

    @Test
    void shouldRejectSeedWhenLocalCiDatabaseCommentIsMissing() throws Exception {
        try (Connection connection =
                        DriverManager.getConnection(
                                hotelCoreJdbcUrl(), "migrator", "migrator_local_dev");
                Statement statement = connection.createStatement()) {
            statement.execute("COMMENT ON DATABASE hotel_core IS NULL");
        }

        try {
            assertThatThrownBy(this::executeSeedScriptDirectlyAsMigrator)
                    .hasStackTraceContaining("division by zero");
        } finally {
            try (Connection restoreConnection =
                            DriverManager.getConnection(
                                    hotelCoreJdbcUrl(), "migrator", "migrator_local_dev");
                    Statement statement = restoreConnection.createStatement()) {
                statement.execute("COMMENT ON DATABASE hotel_core IS 'local-ci-seed'");
            }
        }
    }

    private void assertSeededCountsAndPublicApiResponse() throws Exception {
        Integer activeCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.room_types WHERE active = true",
                        Integer.class);
        Integer inactiveCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.room_types WHERE active = false",
                        Integer.class);
        Integer amenityCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.amenities", Integer.class);
        Integer linkCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM catalog.room_type_amenities", Integer.class);

        assertThat(activeCount).isEqualTo(3);
        assertThat(inactiveCount).isEqualTo(1);
        assertThat(amenityCount).isEqualTo(5);
        assertThat(linkCount).isEqualTo(11);

        HttpResponse<String> response =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create("http://localhost:" + port + "/api/room-types"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("total").asInt()).isEqualTo(3);
        assertThat(json.get("items").size()).isEqualTo(3);

        // First two items share display_order = 10 and are ordered by id ASC (301 then 302), followed by 303
        assertThat(json.get("items").get(0).get("id").asText())
                .isEqualTo("0199f2c0-0000-7000-8000-000000000301");
        assertThat(json.get("items").get(1).get("id").asText())
                .isEqualTo("0199f2c0-0000-7000-8000-000000000302");
        assertThat(json.get("items").get(2).get("id").asText())
                .isEqualTo("0199f2c0-0000-7000-8000-000000000303");
    }
}
