package com.hotel.catalog.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link RoomType} domain rules (AC-1, QA-5, QA-6, QA-7, QA-8, QA-14).
 */
class RoomTypeTest {

    @Test
    void shouldCreateRoomTypeAndSortAmenitiesByCodeAsc_whenValidArgumentsProvided() {
        Amenity wifi = Amenity.create("wifi", "Kablosuz İnternet", "Wireless Internet");
        Amenity jacuzzi = Amenity.create("cave_jacuzzi", "Mağara Jakuzi", "Cave Jacuzzi");
        Amenity terrace = Amenity.create("terrace_view", "Teras Manzarası", "Terrace View");

        RoomType roomType =
                RoomType.create(
                        "  Kemerli Taş Süit  ",
                        "  Arched Stone Suite  ",
                        "  Kapadokya vadisine bakan tarihi taş kemerli süit.  ",
                        "  Historic stone-arched suite overlooking the Cappadocia valley.  ",
                        4,
                        true,
                        10,
                        List.of(wifi, jacuzzi, terrace));

        assertThat(roomType.id()).isNotNull();
        assertThat(roomType.nameTr()).isEqualTo("Kemerli Taş Süit");
        assertThat(roomType.nameEn()).isEqualTo("Arched Stone Suite");
        assertThat(roomType.descriptionTr())
                .isEqualTo("Kapadokya vadisine bakan tarihi taş kemerli süit.");
        assertThat(roomType.descriptionEn())
                .isEqualTo("Historic stone-arched suite overlooking the Cappadocia valley.");
        assertThat(roomType.maxGuests()).isEqualTo(4);
        assertThat(roomType.active()).isTrue();
        assertThat(roomType.displayOrder()).isEqualTo(10);
        assertThat(roomType.amenities())
                .extracting(Amenity::code)
                .containsExactly("cave_jacuzzi", "terrace_view", "wifi");
    }

    @Test
    void shouldDefaultDisplayOrderToZeroAndAcceptBoundaryCapacities_whenAtMinAndMaxLimits() {
        RoomType minGuestsRoom =
                RoomType.create(
                        "Tek Kişilik Oda",
                        "Single Room",
                        "Açıklama TR",
                        "Description EN",
                        1,
                        true,
                        List.of());
        RoomType maxGuestsRoom =
                RoomType.create(
                        "Aile Konağı",
                        "Family Mansion",
                        "Açıklama TR",
                        "Description EN",
                        20,
                        false,
                        0,
                        List.of());

        assertThat(minGuestsRoom.maxGuests()).isEqualTo(1);
        assertThat(minGuestsRoom.displayOrder()).isZero();
        assertThat(maxGuestsRoom.maxGuests()).isEqualTo(20);
        assertThat(maxGuestsRoom.active()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -1, 0, 21, 100})
    void shouldThrowInvalidMaxGuests_whenMaxGuestsOutside1To20Range(int invalidMaxGuests) {
        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        invalidMaxGuests,
                                        true,
                                        0,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex -> {
                            DomainError error = (DomainError) ex;
                            assertThat(error.code())
                                    .isEqualTo(CatalogErrorCodes.ROOM_TYPE_INVALID_MAX_GUESTS);
                            assertThat(error.category()).isEqualTo(DomainErrorCategory.VALIDATION);
                        });
    }

    @Test
    void shouldThrowInvalidDisplayOrder_whenDisplayOrderIsNegative() {
        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        -1,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex -> {
                            DomainError error = (DomainError) ex;
                            assertThat(error.code())
                                    .isEqualTo(CatalogErrorCodes.ROOM_TYPE_INVALID_DISPLAY_ORDER);
                            assertThat(error.category()).isEqualTo(DomainErrorCategory.VALIDATION);
                        });
    }

    @Test
    void shouldThrowRequiredError_whenNameOrDescriptionIsNullOrBlank() {
        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "   ",
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_NAME_REQUIRED);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        null,
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_NAME_REQUIRED);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "  ",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_REQUIRED);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama",
                                        null,
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_REQUIRED);
    }

    @Test
    void shouldThrowInvalidError_whenNameOrDescriptionViolatesLengthHtmlOrControlCharRules() {
        String nameTooLong = "N".repeat(121);
        String descTooLong = "D".repeat(2001);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        nameTooLong,
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_NAME_INVALID);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş <b>Oda</b>",
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_NAME_INVALID);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        descTooLong,
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_INVALID);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama",
                                        "Invalid\u0000Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_INVALID);

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama\u007F",
                                        "Description",
                                        2,
                                        true,
                                        List.of()))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(CatalogErrorCodes.ROOM_TYPE_DESCRIPTION_INVALID);
    }

    @Test
    void shouldThrowConflictError_whenDuplicateAmenityCodeProvided() {
        Amenity first = Amenity.create("wifi", "Kablosuz İnternet", "Wi-Fi");
        Amenity duplicate = Amenity.create("wifi", "Hızlı İnternet", "Fast Wi-Fi");

        assertThatThrownBy(
                        () ->
                                RoomType.create(
                                        "Taş Oda",
                                        "Stone Room",
                                        "Açıklama",
                                        "Description",
                                        2,
                                        true,
                                        List.of(first, duplicate)))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex -> {
                            DomainError error = (DomainError) ex;
                            assertThat(error.code())
                                    .isEqualTo(CatalogErrorCodes.AMENITY_CODE_DUPLICATE);
                            assertThat(error.category()).isEqualTo(DomainErrorCategory.CONFLICT);
                        });
    }
}
