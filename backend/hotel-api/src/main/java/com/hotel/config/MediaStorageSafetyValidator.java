package com.hotel.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails fast when media storage starts outside local/CI/test with development defaults. Public
 * media URLs and presigned uploads must never point at a development MinIO instance in production.
 */
@Component
@Profile("!local & !ci & !test")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MediaStorageSafetyValidator {

    static final String LOCAL_DEFAULT_ACCESS_KEY = "minioadmin";
    static final String LOCAL_DEFAULT_SECRET_KEY = "minioadmin_local_dev";
    private static final int MIN_SECRET_LENGTH = 16;

    public MediaStorageSafetyValidator(
            @Value("${hotel.media.s3.access-key:}") String accessKey,
            @Value("${hotel.media.s3.secret-key:}") String secretKey,
            @Value("${hotel.media.s3.endpoint:}") String endpoint,
            @Value("${hotel.media.s3.public-endpoint:}") String publicEndpoint,
            @Value("${hotel.media.public-base-url:}") String publicBaseUrl,
            @Value("${hotel.media.s3.bucket:}") String bucket) {
        if (LOCAL_DEFAULT_ACCESS_KEY.equals(accessKey)
                || LOCAL_DEFAULT_SECRET_KEY.equals(secretKey)
                || secretKey == null
                || secretKey.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "MEDIA_S3_ACCESS_KEY and MEDIA_S3_SECRET_KEY must be set to non-default values "
                            + "outside the local/CI profiles.");
        }
        if (isInsecure(endpoint) || isInsecure(publicEndpoint) || isInsecure(publicBaseUrl)) {
            throw new IllegalStateException(
                    "Media storage endpoints must use HTTPS and must not point at localhost "
                            + "outside the local/CI profiles.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("MEDIA_S3_BUCKET must be configured.");
        }
    }

    private static boolean isInsecure(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("https://")
                || normalized.contains("localhost")
                || normalized.contains("127.0.0.1");
    }
}
