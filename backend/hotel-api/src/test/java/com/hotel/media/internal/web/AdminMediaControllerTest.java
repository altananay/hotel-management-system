package com.hotel.media.internal.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.catalog.api.ActiveRoomType;
import com.hotel.catalog.api.RoomTypeId;
import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.media.internal.application.MediaAdminService;
import com.hotel.media.internal.application.MediaAdminService.PageResult;
import com.hotel.media.internal.application.MediaStoragePort;
import com.hotel.media.internal.application.MediaUploadService;
import com.hotel.web.AdminJwtAuthenticationEntryPoint;
import com.hotel.web.AdminJwtTestConfiguration;
import com.hotel.web.GlobalExceptionHandler;
import com.hotel.web.ProblemErrorController;
import com.hotel.web.RequestIdFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice tests for admin media authorization and the room-type option endpoint (AC-4, D1). */
@WebMvcTest(controllers = {AdminMediaController.class, ProblemErrorController.class})
@Import({
    SecurityConfig.class,
    AdminJwtTestConfiguration.class,
    AdminJwtAuthenticationEntryPoint.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class,
    ClockConfig.class
})
class AdminMediaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MediaUploadService uploadService;
    @MockitoBean private MediaAdminService adminService;
    @MockitoBean private MediaStoragePort storage;

    @Test
    void shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/admin-api/media"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.unauthorized"));

        verifyNoInteractions(adminService, uploadService);
    }

    @Test
    void shouldReturnForbiddenProblemDetail_whenRoleIsFrontDesk() throws Exception {
        String token = token("FRONT_DESK");

        mockMvc.perform(
                        get("/admin-api/media")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.forbidden"));

        mockMvc.perform(
                        post("/admin-api/media/0199f2c0-0000-7000-8000-000000000401/publish")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    void shouldListEmptyPageForManager() throws Exception {
        when(adminService.list(null, null, false, 0, 20))
                .thenReturn(new PageResult(List.of(), 0, 20, 0));

        mockMvc.perform(
                        get("/admin-api/media")
                                .header("Authorization", "Bearer " + token("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldExposeActiveRoomTypesForMediaAssignment() throws Exception {
        when(adminService.activeRoomTypes())
                .thenReturn(
                        List.of(
                                new ActiveRoomType(
                                        RoomTypeId.of(
                                                UUID.fromString(
                                                        "0199f2c0-0000-7000-8000-000000000301")),
                                        "Kemerli Mağara Süit",
                                        "Arched Cave Suite")));

        mockMvc.perform(
                        get("/admin-api/media/room-types")
                                .header("Authorization", "Bearer " + token("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].id")
                                .value("0199f2c0-0000-7000-8000-000000000301"))
                .andExpect(jsonPath("$[0].nameTr").value("Kemerli Mağara Süit"))
                .andExpect(jsonPath("$[0].nameEn").value("Arched Cave Suite"));
    }

    @Test
    void shouldAllowManagerToPublish() throws Exception {
        UUID mediaId = UUID.fromString("0199f2c0-0000-7000-8000-000000000401");

        mockMvc.perform(
                        post("/admin-api/media/{id}/publish", mediaId)
                                .header("Authorization", "Bearer " + token("MANAGER")))
                .andExpect(status().isOk());

        verify(adminService).publish(mediaId);
    }

    private static String token(String role) throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(300);
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer("admin-api")
                        .audience("hotel-admin-api")
                        .subject(UUID.randomUUID().toString())
                        .issueTime(Date.from(expiresAt.minusSeconds(300)))
                        .expirationTime(Date.from(expiresAt))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("roles", List.of(role))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(AdminJwtTestConfiguration.rsaKey().getKeyID())
                                .build(),
                        claims);
        jwt.sign(new RSASSASigner(AdminJwtTestConfiguration.rsaKey()));
        return jwt.serialize();
    }
}
