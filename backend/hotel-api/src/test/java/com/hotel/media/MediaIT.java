package com.hotel.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.GpsDirectory;
import com.hotel.media.api.MediaStorageException;
import com.hotel.media.internal.application.MediaAdminService;
import com.hotel.media.internal.application.MediaAdminService.UpdateCommand;
import com.hotel.media.internal.application.MediaPublicCleanupService;
import com.hotel.media.internal.application.MediaStoragePort;
import com.hotel.media.internal.application.MediaUploadService;
import com.hotel.media.internal.application.MediaUploadService.StartCommand;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaStatus;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.media.internal.persistence.MediaAssetEntity;
import com.hotel.media.internal.persistence.MediaAssetJpaRepository;
import com.hotel.media.internal.persistence.MediaVariantEntity;
import com.hotel.media.internal.persistence.MediaVariantJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests against real PostgreSQL and MinIO covering presigned upload, async processing,
 * private/public object separation, public API visibility, unpublish/archive and schema constraints
 * (AC-2, AC-3, AC-5, AC-6).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(120)
class MediaIT {

    private static final String BUCKET = "hotel-media";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin_local_dev";

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

    @Container
    static final GenericContainer<?> MINIO =
            new GenericContainer<>("alpine/minio:RELEASE.2025-10-15T17-29-55Z")
                    .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
                    .withCreateContainerCmdModifier(cmd -> cmd.withUser("0:0"))
                    .withCommand("server", "/data")
                    .withExposedPorts(9000)
                    .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    private static S3Client testS3;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired private MediaUploadService uploadService;
    @Autowired private MediaAdminService adminService;
    @Autowired private MediaPublicCleanupService publicCleanupService;
    @MockitoSpyBean private MediaStoragePort storage;
    @Autowired private MediaAssetJpaRepository assetRepository;
    @Autowired private MediaVariantJpaRepository variantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    private static String minioUrl() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }

    private static String hotelCoreJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/hotel_core",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));
    }

    @BeforeAll
    static void migrateSchemaAndInitializeBucket() {
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

        testS3 =
                S3Client.builder()
                        .endpointOverride(URI.create(minioUrl()))
                        .region(Region.US_EAST_1)
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                        .serviceConfiguration(
                                S3Configuration.builder().pathStyleAccessEnabled(true).build())
                        .build();
        testS3.createBucket(builder -> builder.bucket(BUCKET));
        testS3.putBucketPolicy(
                PutBucketPolicyRequest.builder()
                        .bucket(BUCKET)
                        .policy(
                                """
                                {
                                  "Version": "2012-10-17",
                                  "Statement": [{
                                    "Effect": "Allow",
                                    "Principal": {"AWS": ["*"]},
                                    "Action": ["s3:GetObject"],
                                    "Resource": ["arn:aws:s3:::hotel-media/public/*"]
                                  }]
                                }
                                """)
                        .build());
    }

    @AfterAll
    static void closeTestClient() {
        if (testS3 != null) {
            testS3.close();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MediaIT::hotelCoreJdbcUrl);
        registry.add("spring.datasource.username", () -> "app");
        registry.add("spring.datasource.password", () -> "app_local_dev");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("hotel.media.s3.endpoint", MediaIT::minioUrl);
        registry.add("hotel.media.s3.public-endpoint", MediaIT::minioUrl);
        registry.add("hotel.media.s3.access-key", () -> ACCESS_KEY);
        registry.add("hotel.media.s3.secret-key", () -> SECRET_KEY);
        registry.add("hotel.media.s3.bucket", () -> BUCKET);
        registry.add("hotel.media.public-base-url", () -> minioUrl() + "/hotel-media/public");
        registry.add("hotel.media.processing.recovery-initial-delay", () -> "1h");
        registry.add("hotel.media.processing.recovery-interval", () -> "1h");
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM media.media_public_cleanup_tasks");
        jdbcTemplate.update("DELETE FROM media.media_variants");
        jdbcTemplate.update("DELETE FROM media.media_assets");
        jdbcTemplate.update("DELETE FROM catalog.room_type_amenities");
        jdbcTemplate.update("DELETE FROM catalog.room_types");
        jdbcTemplate.update("DELETE FROM catalog.amenities");
    }

    @Test
    void shouldUploadProcessPublishExposeAndThenHideMedia() throws Exception {
        UUID roomTypeId = insertRoomType(true);
        byte[] jpeg = withExifGps(jpeg(2400, 1600));
        assertThat(
                        ImageMetadataReader.readMetadata(new ByteArrayInputStream(jpeg))
                                .getDirectoriesOfType(GpsDirectory.class))
                .isNotEmpty();

        UUID mediaId = uploadAndWaitReady(jpeg);

        MediaAssetEntity ready = assetRepository.findById(mediaId).orElseThrow();
        assertThat(ready.getStatus()).isEqualTo(MediaStatus.READY);
        assertThat(ready.getWidth()).isEqualTo(2400);
        assertThat(ready.getHeight()).isEqualTo(1600);
        assertThat(variantRepository.findByMediaAssetId(mediaId)).hasSize(6);
        for (MediaVariantEntity variant : variantRepository.findByMediaAssetId(mediaId)) {
            if (variant.getFormat() != MediaVariantFormat.JPEG) {
                continue;
            }
            byte[] variantBytes = storage.get(variant.getPrivateStorageKey());
            assertThat(
                            ImageMetadataReader.readMetadata(
                                            new ByteArrayInputStream(variantBytes))
                                    .getDirectoriesOfType(GpsDirectory.class))
                    .isEmpty();
        }
        assertThat(storage.head("private/" + mediaId + "/original")).isNotNull();
        assertThat(storage.head("private/" + mediaId + "/card.webp")).isNotNull();
        assertThat(storage.head("private/" + mediaId + "/card.jpg")).isNotNull();

        adminService.update(
                mediaId,
                new UpdateCommand(true, roomTypeId, true, "Taş süit", true, "Stone suite"));
        adminService.publish(mediaId);
        adminService.cover(mediaId);

        MediaAssetEntity published = assetRepository.findById(mediaId).orElseThrow();
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.isCover()).isTrue();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM media.media_public_cleanup_tasks",
                                Integer.class))
                .isZero();
        assertThat(variantRepository.findByMediaAssetId(mediaId))
                .allSatisfy(variant -> assertThat(variant.getPublicStorageKey()).isNotNull());

        HttpResponse<String> publicApi =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                baseUrl()
                                                        + "/api/media?roomTypeIds="
                                                        + roomTypeId))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(publicApi.statusCode()).isEqualTo(200);
        assertThat(publicApi.headers().firstValue("Cache-Control").orElseThrow())
                .isEqualTo("no-store");
        JsonNode json = objectMapper.readTree(publicApi.body());
        assertThat(json.get("items").size()).isEqualTo(1);
        assertThat(json.get("items").get(0).get("cover").asBoolean()).isTrue();
        assertThat(json.get("items").get(0).get("variants").get("card").get("format").asText())
                .isEqualTo("webp");
        String publicCardUrl =
                json.get("items").get(0).get("variants").get("card").get("url").asText();
        assertThat(publicCardUrl).startsWith(minioUrl() + "/hotel-media/public/");

        HttpResponse<String> publicObject =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(publicCardUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(publicObject.statusCode()).isEqualTo(200);
        assertThat(publicObject.headers().firstValue("Content-Type").orElseThrow())
                .isEqualTo("image/webp");
        assertThat(publicObject.headers().firstValue("Cache-Control").orElseThrow())
                .contains("immutable");

        HttpResponse<String> privateAnonymous =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                minioUrl()
                                                        + "/hotel-media/private/"
                                                        + mediaId
                                                        + "/card.webp"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(privateAnonymous.statusCode()).isEqualTo(403);

        HttpResponse<String> anonymousPut =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(publicCardUrl))
                                .PUT(HttpRequest.BodyPublishers.ofString("overwrite"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(anonymousPut.statusCode()).isIn(403, 405);

        HttpResponse<String> anonymousList =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(minioUrl() + "/hotel-media?list-type=2"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(anonymousList.statusCode()).isEqualTo(403);

        String signedPrivate =
                storage.presignGet(
                        "private/" + mediaId + "/card.webp", Duration.ofMinutes(1));
        HttpResponse<String> privateSigned =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(signedPrivate)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(privateSigned.statusCode()).isEqualTo(200);

        adminService.unpublish(mediaId);
        HttpResponse<String> hiddenApi =
                httpClient.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                baseUrl()
                                                        + "/api/media?roomTypeIds="
                                                        + roomTypeId))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(objectMapper.readTree(hiddenApi.body()).get("items").size()).isZero();
        HttpResponse<String> hiddenObject =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(publicCardUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(hiddenObject.statusCode()).isIn(403, 404);

        // Re-publish, then archive while published: public copies must be removed.
        adminService.publish(mediaId);
        String republishedCardUrl =
                storage.publicUrl(
                        variantRepository.findByMediaAssetId(mediaId).stream()
                                .filter(
                                        variant ->
                                                variant.getVariant() == MediaVariantType.CARD
                                                        && variant.getFormat()
                                                                == MediaVariantFormat.WEBP)
                                .findFirst()
                                .orElseThrow()
                                .getPublicStorageKey());
        assertThat(republishedCardUrl).isNotEqualTo(publicCardUrl);
        HttpResponse<String> republishedObject =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(republishedCardUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(republishedObject.statusCode()).isEqualTo(200);

        adminService.archive(mediaId);
        HttpResponse<String> archivedObject =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(republishedCardUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(archivedObject.statusCode()).isIn(403, 404);
        MediaAssetEntity archived = assetRepository.findById(mediaId).orElseThrow();
        assertThat(archived.getArchivedAt()).isNotNull();
        assertThat(archived.getPublishedAt()).isNull();
        assertThat(storage.get("private/" + mediaId + "/original")).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidImageAndRemoveThePrivateOriginal() throws Exception {
        byte[] jpeg = jpeg(1200, 800);
        MediaUploadService.StartResult start =
                uploadService.start(new StartCommand("room.jpg", "image/jpeg", jpeg.length));
        assertThat(start.expiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(9)));

        byte[] invalid =
                java.util.Arrays.copyOf(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(),
                        jpeg.length);
        HttpResponse<String> put =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(start.uploadUrl()))
                                .header("Content-Type", "image/jpeg")
                                .PUT(HttpRequest.BodyPublishers.ofByteArray(invalid))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(put.statusCode()).isEqualTo(200);

        assertThatThrownBy(() -> uploadService.complete(start.mediaId()))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_CONTENT_TYPE));

        MediaAssetEntity failed = assetRepository.findById(start.mediaId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(storage.head("private/" + start.mediaId() + "/original")).isNull();
    }

    @Test
    void shouldEnforceSchemaConstraintsForOrderCoverAndAltText() {
        UUID roomTypeId = insertRoomType(true);
        UUID first = UUID.randomUUID();
        insertMedia(first, roomTypeId, 0, false, "Oda", "Room");

        assertThatThrownBy(() -> insertMedia(UUID.randomUUID(), roomTypeId, 0, false, "Oda", "Room"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "UPDATE media.media_assets SET published_at = now(), is_cover = true WHERE id = ?",
                first);
        UUID second = UUID.randomUUID();
        insertMedia(second, roomTypeId, 1, false, "Oda", "Room");
        assertThatThrownBy(
                        () ->
                                jdbcTemplate.update(
                                        "UPDATE media.media_assets "
                                                + "SET published_at = now(), is_cover = true "
                                                + "WHERE id = ?",
                                        second))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(
                        () ->
                                insertMedia(
                                        UUID.randomUUID(), roomTypeId, 2, false, "Oda <x>", "Room"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInactiveRoomTypeAssignment() throws Exception {
        UUID mediaId = uploadAndWaitReady(jpeg(1200, 800));
        UUID inactiveRoomTypeId = insertRoomType(false);

        assertThatThrownBy(
                        () ->
                                adminService.update(
                                        mediaId,
                                        new UpdateCommand(
                                                true, inactiveRoomTypeId, false, null, false, null)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ROOM_TYPE_NOT_FOUND));
    }

    @Test
    void shouldCleanupPublicCopiesWhenPublishFailsMidCopy() throws Exception {
        UUID roomTypeId = insertRoomType(true);
        UUID mediaId = uploadAndWaitReady(jpeg(1200, 800));
        adminService.update(
                mediaId,
                new UpdateCommand(true, roomTypeId, false, null, false, null));

        AtomicInteger copies = new AtomicInteger();
        List<String> copiedKeys = new CopyOnWriteArrayList<>();
        doAnswer(
                        invocation -> {
                            if (copies.incrementAndGet() == 2) {
                                throw new MediaStorageException(
                                        "simulated copy failure", new RuntimeException());
                            }
                            Object result = invocation.callRealMethod();
                            copiedKeys.add(invocation.getArgument(1));
                            return result;
                        })
                .when(storage)
                .copy(anyString(), anyString());

        assertThatThrownBy(() -> adminService.publish(mediaId))
                .isInstanceOf(MediaStorageException.class);

        // afterRollback must have removed the first copied object and its intent
        // before any manual cleanup call.
        String firstCopiedKey = copiedKeys.get(0);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM media.media_public_cleanup_tasks "
                                        + "WHERE public_storage_key = ?",
                                Integer.class,
                                firstCopiedKey))
                .isZero();
        assertThat(storage.head(firstCopiedKey)).isNull();

        List<String> plannedKeys =
                jdbcTemplate.queryForList(
                        "SELECT public_storage_key FROM media.media_public_cleanup_tasks",
                        String.class);
        assertThat(plannedKeys).isNotEmpty();
        publicCleanupService.processNow(plannedKeys);

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM media.media_public_cleanup_tasks",
                                Integer.class))
                .isZero();
        assertThat(plannedKeys)
                .allSatisfy(key -> assertThat(storage.head(key)).isNull());
        assertThat(assetRepository.findById(mediaId).orElseThrow().getPublishedAt()).isNull();
    }

    @Test
    void shouldRejectExpiredUploadCompletion() {
        UUID mediaId = UUID.randomUUID();
        assetRepository.save(
                new MediaAssetEntity(
                        mediaId,
                        com.hotel.media.internal.domain.MediaType.PHOTO,
                        "private/" + mediaId + "/original",
                        "image/jpeg",
                        1000,
                        Instant.now().minus(Duration.ofMinutes(11))));

        assertThatThrownBy(() -> uploadService.complete(mediaId))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.UPLOAD_EXPIRED));
    }

    @Test
    void shouldReorderAllNonArchivedMediaAtomically() {
        UUID roomTypeId = insertRoomType(true);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        insertMedia(first, roomTypeId, 0, false, "Oda", "Room");
        insertMedia(second, roomTypeId, 1, false, "Oda", "Room");

        adminService.reorder(roomTypeId, List.of(second, first));

        assertThat(
                        jdbcTemplate.queryForList(
                                "SELECT display_order FROM media.media_assets "
                                        + "WHERE room_type_id = ? ORDER BY display_order",
                                Integer.class,
                                roomTypeId))
                .containsExactly(0, 1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT display_order FROM media.media_assets WHERE id = ?",
                                Integer.class,
                                second))
                .isZero();
    }

    @Test
    void shouldSerializeConcurrentCoverChangesWithoutDeadlockOr500() throws Exception {
        UUID roomTypeId = insertRoomType(true);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        insertMedia(first, roomTypeId, 0, false, "Oda", "Room");
        insertMedia(second, roomTypeId, 1, false, "Oda", "Room");
        jdbcTemplate.update(
                "UPDATE media.media_assets SET published_at = now() WHERE id IN (?, ?)",
                first,
                second);

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start =
                new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Future<String> firstCall =
                    pool.submit(
                            () -> {
                                start.await();
                                try {
                                    adminService.cover(first);
                                    return "ok";
                                } catch (DomainError error) {
                                    return error.code();
                                }
                            });
            java.util.concurrent.Future<String> secondCall =
                    pool.submit(
                            () -> {
                                start.await();
                                try {
                                    adminService.cover(second);
                                    return "ok";
                                } catch (DomainError error) {
                                    return error.code();
                                }
                            });
            start.countDown();
            assertThat(firstCall.get()).isIn("ok", MediaErrorCodes.COVER_CONFLICT);
            assertThat(secondCall.get()).isIn("ok", MediaErrorCodes.COVER_CONFLICT);
        } finally {
            pool.shutdownNow();
        }

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM media.media_assets "
                                        + "WHERE room_type_id = ? AND is_cover = TRUE "
                                        + "AND archived_at IS NULL",
                                Integer.class,
                                roomTypeId))
                .isEqualTo(1);
    }

    @Test
    void shouldFenceLeaseAndMarkExhaustedProcessingFailed() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        mediaId,
                        com.hotel.media.internal.domain.MediaType.PHOTO,
                        "private/" + mediaId + "/original",
                        "image/jpeg",
                        1000,
                        Instant.now());
        asset.markProcessing("image/jpeg", 1000, 1200, 800, Instant.now());
        assetRepository.save(asset);
        UUID claimed = UUID.randomUUID();
        UUID stale = UUID.randomUUID();
        assetRepository.claimForProcessing(
                mediaId,
                MediaStatus.PROCESSING,
                Instant.now(),
                Instant.now().minus(Duration.ofMinutes(10)),
                claimed,
                3);

        assertThat(assetRepository.markReadyIfClaimed(mediaId, stale, Instant.now()))
                .isZero();
        assertThat(assetRepository.findById(mediaId).orElseThrow().getStatus())
                .isEqualTo(MediaStatus.PROCESSING);
        assertThat(assetRepository.releaseProcessingClaim(mediaId, stale, Instant.now()))
                .isZero();

        jdbcTemplate.update(
                "UPDATE media.media_assets SET processing_attempts = 3, "
                        + "processing_started_at = now() - interval '11 minutes' WHERE id = ?",
                mediaId);
        int exhausted =
                assetRepository.markFailedIfExhausted(
                        mediaId,
                        MediaErrorCodes.PROCESSING_FAILED,
                        Instant.now(),
                        Instant.now().minus(Duration.ofMinutes(10)),
                        3);
        assertThat(exhausted).isEqualTo(1);
        MediaAssetEntity failed = assetRepository.findById(mediaId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(MediaStatus.FAILED);
        assertThat(failed.getProcessingLeaseId()).isNull();
    }

    @Test
    void shouldExcludeArchivedProcessingMediaFromClaimAndRecovery() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity asset =
                new MediaAssetEntity(
                        mediaId,
                        com.hotel.media.internal.domain.MediaType.PHOTO,
                        "private/" + mediaId + "/original",
                        "image/jpeg",
                        1000,
                        Instant.now());
        asset.markProcessing("image/jpeg", 1000, 1200, 800, Instant.now());
        asset.archive(Instant.now());
        assetRepository.save(asset);

        int claimed =
                assetRepository.claimForProcessing(
                        mediaId,
                        MediaStatus.PROCESSING,
                        Instant.now(),
                        Instant.now().minus(Duration.ofMinutes(10)),
                        UUID.randomUUID(),
                        3);

        assertThat(claimed).isZero();
        assertThat(
                        assetRepository.findStaleProcessing(
                                MediaStatus.PROCESSING,
                                Instant.now().plus(Duration.ofMinutes(1))))
                .isEmpty();
    }

    private UUID uploadAndWaitReady(byte[] jpeg) throws Exception {
        MediaUploadService.StartResult start =
                uploadService.start(new StartCommand("room.jpg", "image/jpeg", jpeg.length));
        HttpResponse<String> put =
                httpClient.send(
                        HttpRequest.newBuilder(URI.create(start.uploadUrl()))
                                .header("Content-Type", "image/jpeg")
                                .PUT(HttpRequest.BodyPublishers.ofByteArray(jpeg))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(put.statusCode()).isEqualTo(200);

        uploadService.complete(start.mediaId());
        for (int attempt = 0; attempt < 100; attempt++) {
            MediaAssetEntity asset = assetRepository.findById(start.mediaId()).orElseThrow();
            if (asset.getStatus() == MediaStatus.READY) {
                return start.mediaId();
            }
            if (asset.getStatus() == MediaStatus.FAILED) {
                throw new AssertionError("Media processing failed: " + asset.getFailureCode());
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Media did not become READY in time.");
    }

    private UUID insertRoomType(boolean active) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO catalog.room_types "
                        + "(id, name_tr, name_en, description_tr, description_en, max_guests, active, display_order) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                "Kemerli Mağara Süit",
                "Arched Cave Suite",
                "Taş süit.",
                "Stone suite.",
                2,
                active,
                0);
        return id;
    }

    private void insertMedia(
            UUID id,
            UUID roomTypeId,
            int order,
            boolean cover,
            String altTr,
            String altEn) {
        jdbcTemplate.update(
                "INSERT INTO media.media_assets "
                        + "(id, type, status, storage_key, mime_type, size_bytes, room_type_id, "
                        + "display_order, is_cover, alt_text_tr, alt_text_en, created_at, updated_at) "
                        + "VALUES (?, 'PHOTO', 'READY', ?, 'image/jpeg', 1000, ?, ?, ?, ?, ?, now(), now())",
                id,
                "private/" + id + "/original",
                roomTypeId,
                order,
                cover,
                altTr,
                altEn);
    }

    /** Inserts a minimal EXIF APP1 segment with orientation and one GPS tag into a JPEG. */
    private static byte[] withExifGps(byte[] jpeg) throws Exception {
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(tiff);
        data.write(new byte[] {'I', 'I', 0x2A, 0x00});
        data.writeInt(Integer.reverseBytes(8));
        data.writeShort(Short.reverseBytes((short) 2));
        writeEntry(data, 0x0112, 3, 1, 6);
        writeEntry(data, 0x8825, 4, 1, 38);
        data.writeInt(0);
        data.writeShort(Short.reverseBytes((short) 1));
        writeEntry(data, 0x0001, 2, 2, 'N');
        data.writeInt(0);
        data.flush();

        ByteArrayOutputStream app1 = new ByteArrayOutputStream();
        DataOutputStream segment = new DataOutputStream(app1);
        segment.writeByte(0xFF);
        segment.writeByte(0xE1);
        segment.writeShort(2 + 6 + tiff.size());
        segment.write(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        segment.write(tiff.toByteArray());
        segment.flush();

        byte[] result = new byte[jpeg.length + app1.size()];
        result[0] = jpeg[0];
        result[1] = jpeg[1];
        System.arraycopy(app1.toByteArray(), 0, result, 2, app1.size());
        System.arraycopy(jpeg, 2, result, 2 + app1.size(), jpeg.length - 2);
        return result;
    }

    private static void writeEntry(
            DataOutputStream data, int tag, int type, int count, int value) throws Exception {
        data.writeShort(Short.reverseBytes((short) tag));
        data.writeShort(Short.reverseBytes((short) type));
        data.writeInt(Integer.reverseBytes(count));
        if (type == 3) {
            data.writeShort(Short.reverseBytes((short) value));
            data.writeShort(0);
        } else {
            data.writeInt(Integer.reverseBytes(value));
        }
    }

    private static byte[] jpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(140, 110, 90));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(220, 190, 150));
            graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpeg", output)) {
            throw new IllegalStateException("JPEG writer is unavailable");
        }
        return output.toByteArray();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
