package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.scaffold.AdminSampleItemsController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests for {@code /admin-api/**} prefix, X-Request-Id, RFC 7807 error envelope, and scaffold
 * endpoint on {@code admin-api} (AC-6, AC-11).
 */
@WebMvcTest(
        controllers = {
            AdminSampleItemsController.class,
            AdminApiSurfaceAndErrorHandlingTest.AdminTestProbeController.class
        })
@Import({
    SecurityConfig.class,
    ClockConfig.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class,
    AdminApiSurfaceAndErrorHandlingTest.AdminTestProbeController.class
})
class AdminApiSurfaceAndErrorHandlingTest {

    @Autowired private MockMvc mockMvc;

    @RestController
    static class AdminTestProbeController {

        record ProbeInput(@NotBlank String name) {}

        @PostMapping("/admin-api/test/validation")
        public void validateInput(@Valid @RequestBody ProbeInput input) {}

        @GetMapping("/admin-api/test/internal-error")
        public void throwUnexpectedError() {
            throw new IllegalStateException(
                    "Sensitive internal admin detail in "
                            + AdminTestProbeController.class.getName());
        }
    }

    @Test
    void shouldReturnEmptyItemsList_whenCallingAdminSampleItemsScaffoldEndpoint()
            throws Exception {
        mockMvc.perform(get("/admin-api/system/sample-items"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldAcceptValidRequestIdOrGenerateUuidV7_whenInspectingRequestIdHeader()
            throws Exception {
        String customRequestId = "admin-trace-2026-1018";
        mockMvc.perform(
                        get("/admin-api/system/sample-items")
                                .header("X-Request-Id", customRequestId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", customRequestId));

        MvcResult generatedResult =
                mockMvc.perform(
                                get("/admin-api/system/sample-items")
                                        .header("X-Request-Id", "short"))
                        .andExpect(status().isOk())
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

        mockMvc.perform(get("/admin-api/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("system.not_found"));

        mockMvc.perform(post("/admin-api/system/sample-items"))
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
}
