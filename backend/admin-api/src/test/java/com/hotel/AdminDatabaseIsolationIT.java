package com.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
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
 * Integration test verifying {@code admin-api} connects exclusively to {@code hotel_admin},
 * migrates via {@code migrator}, runs as {@code app} with DDL rejected, and exposes only {@code
 * /actuator/health} (AC-5, AC-6, AC-12).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminDatabaseIsolationIT {

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

    private static String hotelAdminJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/hotel_admin",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));
    }

    @BeforeAll
    static void runMigrationsAsMigrator() {
        Flyway.configure()
                .dataSource(hotelAdminJdbcUrl(), "migrator", "migrator_local_dev")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AdminDatabaseIsolationIT::hotelAdminJdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private Environment environment;
    @LocalServerPort private int port;

    @Test
    void shouldConnectToHotelAdminDatabase_whenApplicationStarts() {
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");

        String currentDb = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String currentUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);

        assertThat(currentDb).isEqualTo("hotel_admin");
        assertThat(currentUser).isEqualTo("app");
    }

    @Test
    void shouldRejectDdlStatements_whenExecutedByRuntimeAppUserInAdminDb() {
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.execute(
                                        "CREATE TABLE admin_platform.unauthorized_table (id INT PRIMARY KEY)"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void shouldExposeOnlyHealthEndpointWithoutDetails_whenQueryingAdminActuator()
            throws Exception {
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
