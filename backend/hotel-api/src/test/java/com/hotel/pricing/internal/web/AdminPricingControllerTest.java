package com.hotel.pricing.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.pricing.api.MissingPrice;
import com.hotel.pricing.api.PricingDefaultIncompleteException;
import com.hotel.pricing.internal.application.PricingAdminQueryService;
import com.hotel.pricing.internal.application.PricingAdminService;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
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

/** Web slice tests for admin pricing authorization and problem envelopes (AC-2). */
@WebMvcTest(
        controllers = {
            AdminRatePlanController.class,
            AdminPricingSettingsController.class,
            AdminManualOverrideController.class,
            ProblemErrorController.class
        })
@Import({
    SecurityConfig.class,
    AdminJwtTestConfiguration.class,
    AdminJwtAuthenticationEntryPoint.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class,
    ClockConfig.class,
    PricingWebMapper.class
})
class AdminPricingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PricingAdminService adminService;
    @MockitoBean private PricingAdminQueryService queryService;

    @Test
    void shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/admin-api/pricing/rate-plans"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.unauthorized"));
    }

    @Test
    void shouldReturn403WithSecurityForbidden_whenFrontDeskCallsPricing() throws Exception {
        String token = token("FRONT_DESK");
        mockMvc.perform(
                        get("/admin-api/pricing/rate-plans")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("security.forbidden"));
        mockMvc.perform(
                        get("/admin-api/pricing/settings")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/admin-api/pricing/manual-fx-rates")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteBaseAndSeasonPriceRowsForManager() throws Exception {
        UUID planId = UUID.fromString("0199f2c0-0000-7000-8000-000000000701");
        UUID seasonId = UUID.fromString("0199f2c0-0000-7000-8000-000000000702");
        UUID priceRowId = UUID.fromString("0199f2c0-0000-7000-8000-000000000703");
        String token = token("MANAGER");

        mockMvc.perform(
                        delete(
                                        "/admin-api/pricing/rate-plans/"
                                                + planId
                                                + "/prices/"
                                                + priceRowId)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        delete(
                                        "/admin-api/pricing/seasons/"
                                                + seasonId
                                                + "/prices/"
                                                + priceRowId)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404ForUnknownOrWrongParentPriceRow_whenDeleting() throws Exception {
        UUID planId = UUID.fromString("0199f2c0-0000-7000-8000-000000000701");
        UUID seasonId = UUID.fromString("0199f2c0-0000-7000-8000-000000000702");
        UUID priceRowId = UUID.fromString("0199f2c0-0000-7000-8000-000000000703");
        doThrow(
                        new DomainError(
                                PricingErrorCodes.RATE_PLAN_PRICE_NOT_FOUND,
                                "Base price row was not found.",
                                DomainErrorCategory.NOT_FOUND))
                .when(adminService)
                .deleteBasePrice(any(), any());
        doThrow(
                        new DomainError(
                                PricingErrorCodes.SEASON_PRICE_NOT_FOUND,
                                "Season price row was not found.",
                                DomainErrorCategory.NOT_FOUND))
                .when(adminService)
                .deleteSeasonPrice(any(), any());
        String token = token("SUPER_ADMIN");

        mockMvc.perform(
                        delete(
                                        "/admin-api/pricing/rate-plans/"
                                                + planId
                                                + "/prices/"
                                                + priceRowId)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("pricing.rate_plan_price_not_found"));
        mockMvc.perform(
                        delete(
                                        "/admin-api/pricing/seasons/"
                                                + seasonId
                                                + "/prices/"
                                                + priceRowId)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("pricing.season_price_not_found"));
    }

    @Test
    void shouldForbidFrontDeskAndAnonymous_whenDeletingPriceRows() throws Exception {
        UUID planId = UUID.fromString("0199f2c0-0000-7000-8000-000000000701");
        UUID priceRowId = UUID.fromString("0199f2c0-0000-7000-8000-000000000703");
        mockMvc.perform(
                        delete(
                                "/admin-api/pricing/rate-plans/"
                                        + planId
                                        + "/prices/"
                                        + priceRowId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        delete(
                                        "/admin-api/pricing/rate-plans/"
                                                + planId
                                                + "/prices/"
                                                + priceRowId)
                                .header("Authorization", "Bearer " + token("FRONT_DESK")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("security.forbidden"));
    }

    @Test
    void shouldAllowManagerToReadPricing() throws Exception {
        when(queryService.listRatePlans()).thenReturn(List.of());
        mockMvc.perform(
                        get("/admin-api/pricing/rate-plans")
                                .header("Authorization", "Bearer " + token("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn503WithPricingCode_whenDefaultPlanIsRequired() throws Exception {
        when(queryService.settings())
                .thenThrow(
                        new DomainError(
                                PricingErrorCodes.DEFAULT_RATE_PLAN_REQUIRED,
                                "No default plan.",
                                DomainErrorCategory.INTERNAL));
        mockMvc.perform(
                        get("/admin-api/pricing/settings")
                                .header("Authorization", "Bearer " + token("MANAGER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("pricing.default_rate_plan_required"));
    }

    @Test
    void shouldReturn409WithMissingList_whenDefaultPlanIsIncomplete() throws Exception {
        UUID planId = UUID.fromString("0199f2c0-0000-7000-8000-000000000701");
        UUID roomTypeId = UUID.fromString("0199f2c0-0000-7000-8000-000000000301");
        when(adminService.setDefault(planId))
                .thenThrow(
                        new PricingDefaultIncompleteException(
                                List.of(new MissingPrice(roomTypeId, 2))));
        mockMvc.perform(
                        put("/admin-api/pricing/rate-plans/" + planId + "/default")
                                .header("Authorization", "Bearer " + token("SUPER_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("pricing.default_rate_plan_incomplete"))
                .andExpect(jsonPath("$.missing[0].roomTypeId").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.missing[0].guests").value(2));
    }

    private String token(String role) throws Exception {
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
