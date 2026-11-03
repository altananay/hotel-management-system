package com.hotel.media.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for upload validation rules (AC-1, AC-2, Q4). */
class MediaRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "IMAGE/PNG", " image/webp "})
    void shouldNormalizeSupportedContentTypes(String raw) {
        assertThat(MediaRules.normalizeContentType(raw))
                .isEqualTo(raw.trim().toLowerCase(Locale.ROOT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "image/svg+xml", "video/mp4", "text/plain"})
    void shouldRejectUnsupportedContentTypes(String raw) {
        assertThatThrownBy(() -> MediaRules.normalizeContentType(raw))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_CONTENT_TYPE));
    }

    @Test
    void shouldRejectNullContentType() {
        assertThatThrownBy(() -> MediaRules.normalizeContentType(null))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).category())
                                        .isEqualTo(DomainErrorCategory.VALIDATION));
    }

    @Test
    void shouldAcceptUploadSizeAtBoundaryAndRejectAboveOrZero() {
        MediaRules.validateUploadSize(MediaRules.MAX_UPLOAD_BYTES);

        assertThatThrownBy(() -> MediaRules.validateUploadSize(0))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.FILE_TOO_LARGE));
        assertThatThrownBy(() -> MediaRules.validateUploadSize(MediaRules.MAX_UPLOAD_BYTES + 1))
                .isInstanceOf(DomainError.class);
    }

    @Test
    void shouldAcceptMinimumDimensionsAndRejectSmallOrExcessivePixels() {
        MediaRules.validateImageDimensions(800, 600);
        MediaRules.validateImageDimensions(6000, 6000);

        assertThatThrownBy(() -> MediaRules.validateImageDimensions(799, 600))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.IMAGE_TOO_LARGE));
        assertThatThrownBy(() -> MediaRules.validateImageDimensions(800, 599))
                .isInstanceOf(DomainError.class);
        assertThatCode(() -> MediaRules.validateImageDimensions(6000, 6000))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectImagesAbovePixelOrSideLimits() {
        assertThatThrownBy(() -> MediaRules.validateImageDimensions(6001, 600))
                .isInstanceOf(DomainError.class);
        assertThatThrownBy(() -> MediaRules.validateImageDimensions(8000, 5000))
                .isInstanceOf(DomainError.class);
    }

    @Test
    void shouldTrimAltTextAndMapBlankToNull() {
        assertThat(MediaRules.validateAltText("  Taş oda  ")).isEqualTo("Taş oda");
        assertThat(MediaRules.validateAltText("   ")).isNull();
        assertThat(MediaRules.validateAltText(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Oda <script>", "Od\u0001a", "A"})
    void shouldRejectUnsafeOrTooLongAltText(String value) {
        String candidate = value.equals("A") ? "A".repeat(201) : value;

        assertThatThrownBy(() -> MediaRules.validateAltText(candidate))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.ALT_TEXT_INVALID));
    }
}
