package com.hotel.media.internal.domain;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.Locale;
import java.util.Set;

public final class MediaRules {

    public static final long MAX_UPLOAD_BYTES = 15L * 1024 * 1024;
    public static final long MAX_PIXELS = 36_000_000L;
    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 600;
    public static final int MAX_ALT_TEXT_LENGTH = 200;
    public static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private MediaRules() {}

    public static String normalizeContentType(String raw) {
        if (raw == null) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_CONTENT_TYPE,
                    "A supported image content type is required.",
                    DomainErrorCategory.VALIDATION);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new DomainError(
                    MediaErrorCodes.INVALID_CONTENT_TYPE,
                    "Only JPEG, PNG and WebP images are accepted.",
                    DomainErrorCategory.VALIDATION);
        }
        return normalized;
    }

    public static void validateUploadSize(long sizeBytes) {
        if (sizeBytes <= 0 || sizeBytes > MAX_UPLOAD_BYTES) {
            throw new DomainError(
                    MediaErrorCodes.FILE_TOO_LARGE,
                    "The uploaded image exceeds the allowed size.",
                    DomainErrorCategory.VALIDATION);
        }
    }

    public static void validateImageDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (width < MIN_WIDTH
                || height < MIN_HEIGHT
                || width > 6000
                || height > 6000
                || pixels > MAX_PIXELS) {
            throw new DomainError(
                    MediaErrorCodes.IMAGE_TOO_LARGE,
                    "The image dimensions are outside the allowed range.",
                    DomainErrorCategory.VALIDATION);
        }
    }

    public static String validateAltText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_ALT_TEXT_LENGTH
                || trimmed.contains("<")
                || trimmed.contains(">")
                || trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new DomainError(
                    MediaErrorCodes.ALT_TEXT_INVALID,
                    "Alt text must be plain text up to 200 characters.",
                    DomainErrorCategory.VALIDATION);
        }
        return trimmed;
    }
}
