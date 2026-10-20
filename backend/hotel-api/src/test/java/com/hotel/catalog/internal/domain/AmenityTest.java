package com.hotel.catalog.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.catalog.api.AmenityId;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Amenity} domain rules (AC-1, QA-6, QA-8, QA-14).
 */
class AmenityTest {

    @Test
    void shouldCreateAmenityAndTrimNames_whenValidArgumentsProvided() {
        AmenityId id = AmenityId.generate();
        Amenity amenity =
                new Amenity(id, "cave_jacuzzi_1", "  Mağara Jakuzi  ", "  Cave Jacuzzi  ");

        assertThat(amenity.id()).isEqualTo(id);
        assertThat(amenity.code()).isEqualTo("cave_jacuzzi_1");
        assertThat(amenity.nameTr()).isEqualTo("Mağara Jakuzi");
        assertThat(amenity.nameEn()).isEqualTo("Cave Jacuzzi");
    }

    @Test
    void shouldAcceptBoundaryCodeAndNameLengths_whenAtMinAndMaxLimits() {
        String minCode = "ab"; // 2 chars
        String maxCode = "a" + "b".repeat(49); // 50 chars
        String maxName = "A".repeat(80); // 80 chars

        Amenity minAmenity = Amenity.create(minCode, "A", "B");
        Amenity maxAmenity = Amenity.create(maxCode, maxName, maxName);

        assertThat(minAmenity.code()).hasSize(2);
        assertThat(maxAmenity.code()).hasSize(50);
        assertThat(maxAmenity.nameTr()).hasSize(80);
        assertThat(maxAmenity.nameEn()).hasSize(80);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "a",
                "1wifi",
                "_wifi",
                "Wifi",
                "wifi-fast",
                "wifi fast",
                "a12345678901234567890123456789012345678901234567890"
            })
    void shouldRejectInvalidAmenityCode_whenPatternOrLengthViolated(String invalidCode) {
        assertThatThrownBy(() -> Amenity.create(invalidCode, "Wi-Fi", "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex -> {
                            DomainError error = (DomainError) ex;
                            assertThat(error.code())
                                    .isEqualTo(CatalogErrorCodes.AMENITY_CODE_INVALID);
                            assertThat(error.category()).isEqualTo(DomainErrorCategory.VALIDATION);
                        });
    }

    @Test
    void shouldRejectNullAmenityCode_whenCodeIsNull() {
        assertThatThrownBy(() -> Amenity.create(null, "Wi-Fi", "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_CODE_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectBlankAmenityNames_whenNameTrOrNameEnIsBlank(String blank) {
        assertThatThrownBy(() -> Amenity.create("wifi", blank, "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_REQUIRED);

        assertThatThrownBy(() -> Amenity.create("wifi", "Wi-Fi", blank))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_REQUIRED);

        assertThatThrownBy(() -> Amenity.create("wifi", null, "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_REQUIRED);
    }

    @Test
    void shouldRejectAmenityName_whenLengthExceeds80OrContainsHtmlOrControlChars() {
        String tooLong = "A".repeat(81);

        assertThatThrownBy(() -> Amenity.create("wifi", tooLong, "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_INVALID);

        assertThatThrownBy(() -> Amenity.create("wifi", "Wi-Fi <script>", "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_INVALID);

        assertThatThrownBy(() -> Amenity.create("wifi", "Wi-Fi", "Fast > Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_INVALID);

        // C0 control character (\u0007) and C1 control character (\u0085)
        assertThatThrownBy(() -> Amenity.create("wifi", "Wi\u0007Fi", "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_INVALID);

        assertThatThrownBy(() -> Amenity.create("wifi", "Wi-Fi", "Wi\u0085Fi"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.AMENITY_NAME_INVALID);
    }
}
