package com.hotel.media.internal.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.media.internal.application.MediaPublicQueryService;
import com.hotel.media.internal.application.MediaPublicQueryService.PublicMediaView;
import com.hotel.media.internal.application.MediaPublicQueryService.PublicVariant;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.web.GlobalExceptionHandler;
import com.hotel.web.ProblemErrorController;
import com.hotel.web.RequestIdFilter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice tests for the anonymous public media endpoint (AC-5, Q5). */
@WebMvcTest(controllers = {PublicMediaController.class, ProblemErrorController.class})
@Import({
    SecurityConfig.class,
    ClockConfig.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class
})
class PublicMediaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MediaPublicQueryService publicQueryService;

    @Test
    void shouldReturnOnlyMediaEnvelopeWithCacheHeader_whenCalledAnonymously() throws Exception {
        UUID roomTypeId = UUID.fromString("0199f2c0-0000-7000-8000-000000000301");
        UUID mediaId = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");
        Map<MediaVariantType, PublicVariant> variants = new EnumMap<>(MediaVariantType.class);
        variants.put(
                MediaVariantType.CARD,
                new PublicVariant(
                        MediaVariantType.CARD,
                        "https://cdn.example.com/card.webp",
                        1200,
                        800,
                        "webp"));
        when(publicQueryService.findPublished(List.of(roomTypeId)))
                .thenReturn(
                        List.of(
                                new PublicMediaView(
                                        mediaId,
                                        roomTypeId,
                                        true,
                                        0,
                                        "Taş süit",
                                        "Stone suite",
                                        variants)));

        mockMvc.perform(get("/api/media").param("roomTypeIds", roomTypeId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(mediaId.toString()))
                .andExpect(jsonPath("$.items[0].roomTypeId").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.items[0].cover").value(true))
                .andExpect(jsonPath("$.items[0].altTextTr").value("Taş süit"))
                .andExpect(
                        jsonPath("$.items[0].variants.card.url")
                                .value("https://cdn.example.com/card.webp"))
                .andExpect(jsonPath("$.items[0].variants.card.format").value("webp"))
                .andExpect(jsonPath("$.items[0].variants.thumb").doesNotExist())
                .andExpect(jsonPath("$.items[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.items[0].failureCode").doesNotExist())
                .andExpect(jsonPath("$.items[0].original").doesNotExist());
    }

    @Test
    void shouldReturn400ProblemDetail_whenRoomTypeIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/media").param("roomTypeIds", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation.invalid_request"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verifyNoInteractions(publicQueryService);
    }

    @Test
    void shouldReturn400_whenRoomTypeIdsParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/media"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation.invalid_request"));
    }
}
