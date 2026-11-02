package com.hotel.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminJwtTestController.class)
@Import({
    SecurityConfig.class,
    AdminJwtTestConfiguration.class,
    AdminJwtAuthenticationEntryPoint.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class,
    ClockConfig.class
})
class AdminJwtSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldAllowSuperAdminAndManagerEndpoints_whenJwtIsValid() throws Exception {
        String token = token("SUPER_ADMIN", "admin-api", "hotel-admin-api", Instant.now().plusSeconds(300));

        mockMvc.perform(get("/admin-api/test/authenticated").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin-api/test/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin-api/test/super-admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowManagerEndpointAndForbidSuperAdmin_whenRoleIsManager() throws Exception {
        String token = token("MANAGER", "admin-api", "hotel-admin-api", Instant.now().plusSeconds(300));

        mockMvc.perform(get("/admin-api/test/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin-api/test/super-admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.forbidden"));
    }

    @Test
    void shouldForbidManagerAndSuperAdmin_whenRoleIsFrontDesk() throws Exception {
        String token = token("FRONT_DESK", "admin-api", "hotel-admin-api", Instant.now().plusSeconds(300));

        mockMvc.perform(get("/admin-api/test/authenticated").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin-api/test/manager").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin-api/test/super-admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401ForMissingWrongAudienceWrongIssuerAndExpiredTokens() throws Exception {
        mockMvc.perform(get("/admin-api/test/authenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.unauthorized"))
                .andExpect(header().exists("X-Request-Id"));

        String wrongAudience =
                token("SUPER_ADMIN", "admin-api", "other-audience", Instant.now().plusSeconds(300));
        mockMvc.perform(
                        get("/admin-api/test/authenticated")
                                .header("Authorization", "Bearer " + wrongAudience))
                .andExpect(status().isUnauthorized());

        String wrongIssuer =
                token("SUPER_ADMIN", "other-issuer", "hotel-admin-api", Instant.now().plusSeconds(300));
        mockMvc.perform(
                        get("/admin-api/test/authenticated")
                                .header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized());

        String expired =
                token("SUPER_ADMIN", "admin-api", "hotel-admin-api", Instant.now().minusSeconds(60));
        mockMvc.perform(
                        get("/admin-api/test/authenticated")
                                .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    private String token(String role, String issuer, String audience, Instant expiresAt)
            throws Exception {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(audience)
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
