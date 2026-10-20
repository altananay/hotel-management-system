package com.hotel.catalog.internal.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.catalog.api.AmenityId;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.catalog.internal.application.RoomCatalogQueryService;
import com.hotel.catalog.internal.application.RoomCatalogQueryService.PagedRoomTypesResult;
import com.hotel.catalog.internal.domain.Amenity;
import com.hotel.catalog.internal.domain.RoomType;
import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.web.GlobalExceptionHandler;
import com.hotel.web.ProblemErrorController;
import com.hotel.web.RequestIdFilter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web slice tests for {@link RoomTypeController} verifying public access, pagination bounds,
 * response contract and field exclusions (AC-2, QA-3, QA-4, QA-8).
 */
@WebMvcTest(controllers = {RoomTypeController.class, ProblemErrorController.class})
@Import({
    SecurityConfig.class,
    ClockConfig.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class
})
class RoomTypeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RoomCatalogQueryService roomCatalogQueryService;

    @Test
    void shouldReturnPaginatedEnvelopeWithDefaultPageAndSize_whenCalledAnonymously()
            throws Exception {
        Amenity wifi =
                new Amenity(
                        AmenityId.of(UUID.fromString("0199f2c0-0000-7000-8000-000000000101")),
                        "wifi",
                        "Kablosuz İnternet",
                        "Wireless Internet");
        Amenity jacuzzi =
                new Amenity(
                        AmenityId.of(UUID.fromString("0199f2c0-0000-7000-8000-000000000102")),
                        "cave_jacuzzi",
                        "Mağara Jakuzi",
                        "Cave Jacuzzi");
        RoomType suite =
                new RoomType(
                        RoomTypeId.of(UUID.fromString("0199f2c0-0000-7000-8000-000000000001")),
                        "Kral Mağara Süit",
                        "Royal Cave Suite",
                        "Özel teraslı mağara süit.",
                        "Cave suite with private terrace.",
                        4,
                        true,
                        10,
                        List.of(wifi, jacuzzi));

        when(roomCatalogQueryService.listActiveRoomTypes(0, 20))
                .thenReturn(new PagedRoomTypesResult(List.of(suite), 0, 20, 1L));

        mockMvc.perform(get("/api/room-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(
                        jsonPath("$.items[0].id")
                                .value("0199f2c0-0000-7000-8000-000000000001"))
                .andExpect(jsonPath("$.items[0].nameTr").value("Kral Mağara Süit"))
                .andExpect(jsonPath("$.items[0].nameEn").value("Royal Cave Suite"))
                .andExpect(jsonPath("$.items[0].descriptionTr").value("Özel teraslı mağara süit."))
                .andExpect(
                        jsonPath("$.items[0].descriptionEn")
                                .value("Cave suite with private terrace."))
                .andExpect(jsonPath("$.items[0].maxGuests").value(4))
                .andExpect(jsonPath("$.items[0].amenities.length()").value(2))
                .andExpect(jsonPath("$.items[0].amenities[0].code").value("cave_jacuzzi"))
                .andExpect(jsonPath("$.items[0].amenities[0].nameTr").value("Mağara Jakuzi"))
                .andExpect(jsonPath("$.items[0].amenities[0].nameEn").value("Cave Jacuzzi"))
                .andExpect(jsonPath("$.items[0].amenities[1].code").value("wifi"))
                // Excluded internal fields (QA-3)
                .andExpect(jsonPath("$.items[0].active").doesNotExist())
                .andExpect(jsonPath("$.items[0].displayOrder").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].amenities[0].id").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-5", "0,101", "abc,20", "0,xyz"})
    void shouldReturn400InvalidRequestProblemDetail_whenPageOrSizeIsInvalid(
            String pageParam, String sizeParam) throws Exception {
        mockMvc.perform(
                        get("/api/room-types")
                                .param("page", pageParam)
                                .param("size", sizeParam))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("validation.invalid_request"))
                .andExpect(jsonPath("$.instance").value("/api/room-types"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void shouldIgnoreUnknownQueryParamsIncludingSort_whenListingRoomTypes() throws Exception {
        when(roomCatalogQueryService.listActiveRoomTypes(0, 100))
                .thenReturn(new PagedRoomTypesResult(List.of(), 0, 100, 0L));

        mockMvc.perform(
                        get("/api/room-types")
                                .param("page", "0")
                                .param("size", "100")
                                .param("sort", "nameTr,desc")
                                .param("unknown", "ignored"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.total").value(0));
    }
}
