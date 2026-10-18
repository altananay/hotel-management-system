package com.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Integration test verifying Testcontainers initialization from {@code infra/postgres/init},
 * Flyway migration execution as {@code migrator}, presence of the 6 module schemas in {@code
 * hotel_core}, DDL rejection for the runtime {@code app} role, and Actuator health posture (AC-3,
 * AC-6, AC-12).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseSchemaAndRoleSeparationIT {

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
    static void runMigrationsAsMigrator() {
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
        registry.add("spring.datasource.url", DatabaseSchemaAndRoleSeparationIT::hotelCoreJdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private Environment environment;
    @LocalServerPort private int port;

    @Test
    void shouldCreateSixModuleSchemasAndRunAsAppUserWithFlywayDisabled_whenMigratedByMigrator() {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");

        String currentDb = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String currentUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        assertThat(currentDb).isEqualTo("hotel_core");
        assertThat(currentUser).isEqualTo("app");

        List<String> schemas =
                jdbcTemplate.queryForList(
                        "SELECT schema_name FROM information_schema.schemata "
                                + "WHERE schema_name IN ('catalog','pricing','booking','payment','media','notification','sharedkernel','reporting')",
                        String.class);

        assertThat(schemas)
                .containsExactlyInAnyOrder(
                        "catalog", "pricing", "booking", "payment", "media", "notification")
                .doesNotContain("sharedkernel", "reporting");
    }

    @Test
    void shouldRejectDdlStatements_whenExecutedByRuntimeAppUser() {
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.execute(
                                        "CREATE TABLE catalog.unauthorized_table (id INT PRIMARY KEY)"))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.execute("CREATE SCHEMA unauthorized_schema"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void shouldExposeOnlyHealthEndpointWithoutDetails_whenQueryingActuator() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String baseUrl = "http://localhost:" + port;

        HttpResponse<String> healthResponse =
                client.send(
                        HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(healthResponse.statusCode()).isEqualTo(200);
        assertThat(healthResponse.body())
                .contains("\"status\":\"UP\"")
                .doesNotContain("components", "details", "db", "diskSpace");

        HttpResponse<String> envResponse =
                client.send(
                        HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/env")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(envResponse.statusCode()).isIn(401, 404);
    }
}
