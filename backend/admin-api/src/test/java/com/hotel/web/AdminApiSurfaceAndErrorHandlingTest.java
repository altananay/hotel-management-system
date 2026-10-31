package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.adminidentity.api.AdminIdentityApi;
import com.hotel.adminidentity.api.AdminSessionException;
import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests for {@code /admin-api/**} prefix, deny-by-default surface, X-Request-Id, RFC 7807 error
 * envelope, and scaffold endpoint on {@code admin-api} (AC-6, AC-11, CR-1, CR-4).
 */
@WebMvcTest(controllers = AdminApiSurfaceAndErrorHandlingTest.AdminTestProbeController.class)
@Import({
    SecurityConfig.class,
    ClockConfig.class,
    RequestIdFilter.class,
    AdminProblemWriter.class,
    ClientIpResolver.class,
    GlobalExceptionHandler.class,
    AdminApiSurfaceAndErrorHandlingTest.AdminTestProbeController.class,
    AdminApiSurfaceAndErrorHandlingTest.ProbeSecurityTestConfiguration.class
})
class AdminApiSurfaceAndErrorHandlingTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminIdentityApi identityApi;

    @org.junit.jupiter.api.BeforeEach
    void rejectMissingSession() {
        when(identityApi.validateSession(any(), any()))
                .thenThrow(new AdminSessionException());
    }

    /**
     * Test-only chain: allows reaching the probe controller under {@code /admin-api/test/**} so
     * error handling can be exercised while the real chain stays deny-by-default.
     */
    @TestConfiguration
    static class ProbeSecurityTestConfiguration {

        @Bean
        @Order(0)
        SecurityFilterChain probeSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/admin-api/test/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @RestController
    static class AdminTestProbeController {

        record ProbeInput(@NotBlank String name) {}

        @PostMapping("/admin-api/test/validation")
        public void validateInput(@Valid @RequestBody ProbeInput input) {}

        @GetMapping("/admin-api/test/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access is denied for the current principal.");
        }

        @GetMapping("/admin-api/test/response-status")
        public void throwResponseStatus() {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable");
        }

        @GetMapping("/admin-api/test/internal-error")
        public void throwUnexpectedError() {
            throw new IllegalStateException(
                    "Sensitive internal admin detail in "
                            + AdminTestProbeController.class.getName());
        }
    }

    @Test
    void shouldReturn401ProblemDetail_whenCallingUnknownAdminPath() throws Exception {
        mockMvc.perform(get("/admin-api/unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("security.unauthorized"))
                .andExpect(jsonPath("$.instance").value("/admin-api/unknown"));
    }

    @Test
    void shouldAcceptValidRequestIdOrGenerateUuidV7_whenInspectingRequestIdHeader()
            throws Exception {
        String customRequestId = "admin-trace-2026-1018";
        mockMvc.perform(
                        get("/admin-api/test/access-denied")
                                .header("X-Request-Id", customRequestId))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", customRequestId));

        MvcResult generatedResult =
                mockMvc.perform(
                                get("/admin-api/test/access-denied")
                                        .header("X-Request-Id", "short"))
                        .andExpect(status().isForbidden())
                        .andReturn();

        String generated = generatedResult.getResponse().getHeader("X-Request-Id");
        assertThat(generated)
                .isNotNull()
                .isNotEqualTo("short")
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    @Test
    void shouldReturnStandardProblemDetailEnvelopeWithoutLeakingStackTrace_whenErrorsOccur()
            throws Exception {
        mockMvc.perform(
                        post("/admin-api/test/validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("validation.invalid_request"));

        mockMvc.perform(post("/admin-api/test/access-denied"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("system.method_not_allowed"));

        MvcResult result500 =
                mockMvc.perform(get("/admin-api/test/internal-error"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(
                                content()
                                        .contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.type").value("about:blank"))
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code").value("system.internal_error"))
                        .andExpect(
                                jsonPath("$.detail")
                                        .value("An unexpected internal error occurred."))
                        .andReturn();

        String body500 = result500.getResponse().getContentAsString();
        assertThat(body500)
                .doesNotContain("IllegalStateException")
                .doesNotContain("AdminTestProbeController")
                .doesNotContain("Sensitive internal admin detail");
    }

    @Test
    void shouldPreserveClientErrorStatuses_when415And403And422Occur() throws Exception {
        // 415 -> system.unsupported_media_type
        mockMvc.perform(
                        post("/admin-api/test/validation")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("system.unsupported_media_type"));

        // 403 -> security.forbidden
        mockMvc.perform(get("/admin-api/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("security.forbidden"));

        // 422 -> status preserved
        mockMvc.perform(get("/admin-api/test/response-status"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("system.http_422"));
    }
}
