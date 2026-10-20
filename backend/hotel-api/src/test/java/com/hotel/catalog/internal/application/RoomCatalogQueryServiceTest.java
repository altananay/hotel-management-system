package com.hotel.catalog.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.catalog.internal.application.RoomCatalogQueryService.PagedRoomTypesResult;
import com.hotel.catalog.internal.domain.Amenity;
import com.hotel.catalog.internal.domain.CatalogErrorCodes;
import com.hotel.catalog.internal.domain.RoomType;
import com.hotel.catalog.internal.persistence.AmenityEntity;
import com.hotel.catalog.internal.persistence.AmenityJpaRepository;
import com.hotel.catalog.internal.persistence.RoomTypeEntity;
import com.hotel.catalog.internal.persistence.RoomTypeJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RoomCatalogQueryService} (AC-1, AC-2).
 */
@ExtendWith(MockitoExtension.class)
class RoomCatalogQueryServiceTest {

    @Mock private RoomTypeJpaRepository roomTypeRepository;
    @Mock private AmenityJpaRepository amenityRepository;

    private Clock fixedClock;
    private RoomCatalogQueryService service;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-10-20T09:00:00Z"), ZoneOffset.UTC);
        service = new RoomCatalogQueryService(roomTypeRepository, amenityRepository, fixedClock);
    }

    @Test
    void shouldReturnEmptyPageWithoutQueryingRows_whenActiveCountIsZero() {
        when(roomTypeRepository.countActive()).thenReturn(0L);

        PagedRoomTypesResult result = service.listActiveRoomTypes(0, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.total()).isZero();
        verify(roomTypeRepository, never()).findActivePageOrdered(0L, 20);
    }

    @Test
    void shouldReturnEmptyItemsAndPreserveTotal_whenRequestedPageExceedsLastPage() {
        when(roomTypeRepository.countActive()).thenReturn(3L);

        PagedRoomTypesResult result = service.listActiveRoomTypes(2, 2);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(3L);
        verify(roomTypeRepository, never()).findActivePageOrdered(4L, 2);
    }

    @Test
    void shouldPreserveDatabaseSortOrderAndSortAmenitiesByCodeAsc_whenHydratingActivePage() {
        Instant now = Instant.now(fixedClock);
        UUID firstId = UUID.fromString("0199f2c0-0000-7000-8000-000000000001");
        UUID secondId = UUID.fromString("0199f2c0-0000-7000-8000-000000000002");

        AmenityEntity wifi =
                new AmenityEntity(
                        UUID.fromString("0199f2c0-0000-7000-8000-000000000101"),
                        "wifi",
                        "Kablosuz İnternet",
                        "Wi-Fi",
                        now,
                        now);
        AmenityEntity jacuzzi =
                new AmenityEntity(
                        UUID.fromString("0199f2c0-0000-7000-8000-000000000102"),
                        "cave_jacuzzi",
                        "Mağara Jakuzi",
                        "Cave Jacuzzi",
                        now,
                        now);

        RoomTypeEntity firstEntity =
                new RoomTypeEntity(
                        firstId,
                        "Birinci Oda",
                        "First Room",
                        "Açıklama 1",
                        "Description 1",
                        (short) 2,
                        true,
                        10,
                        now,
                        now,
                        Set.of(wifi, jacuzzi));
        RoomTypeEntity secondEntity =
                new RoomTypeEntity(
                        secondId,
                        "İkinci Oda",
                        "Second Room",
                        "Açıklama 2",
                        "Description 2",
                        (short) 4,
                        true,
                        10,
                        now,
                        now,
                        Set.of(wifi));

        when(roomTypeRepository.countActive()).thenReturn(2L);
        when(roomTypeRepository.findActivePageOrdered(0L, 20))
                .thenReturn(List.of(firstEntity, secondEntity));
        // Return in reversed order from IN-query to prove service preserves SQL page ordering
        when(roomTypeRepository.findAllWithAmenitiesByIdIn(List.of(firstId, secondId)))
                .thenReturn(List.of(secondEntity, firstEntity));

        PagedRoomTypesResult result = service.listActiveRoomTypes(0, 20);

        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.items())
                .extracting(rt -> rt.id().value())
                .containsExactly(firstId, secondId);
        assertThat(result.items().get(0).amenities())
                .extracting(Amenity::code)
                .containsExactly("cave_jacuzzi", "wifi");
    }

    @Test
    void shouldThrowAmenityCodeDuplicateConflict_whenAmenityCodeAlreadyExists() {
        when(amenityRepository.existsByCode("wifi")).thenReturn(true);

        assertThatThrownBy(() -> service.createAmenity("wifi", "Kablosuz İnternet", "Wi-Fi"))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        ex -> {
                            DomainError error = (DomainError) ex;
                            assertThat(error.code())
                                    .isEqualTo(CatalogErrorCodes.AMENITY_CODE_DUPLICATE);
                            assertThat(error.category()).isEqualTo(DomainErrorCategory.CONFLICT);
                        });

        verify(amenityRepository, never()).save(any());
    }
}
