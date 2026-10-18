package com.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotel.config.ClockConfig;
import com.hotel.config.SecurityConfig;
import com.hotel.scaffold.SampleItemsController;
import com.hotel.sharedkernel.api.DomainError;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests for API surface separation, unknown-path handling, CORS, X-Request-Id, RFC 7807 error
 * envelope, domain error categories, /error dispatch and scaffold endpoint on {@code hotel-api}
 * (AC-6, AC-11, CR-1…CR-6).
 */
@WebMvcTest(
        controllers = {
            SampleItemsController.class,
            ProblemErrorController.class,
            ApiSurfaceAndErrorHandlingTest.TestProbeController.class
        })
@Import({
    SecurityConfig.class,
    ClockConfig.class,
    RequestIdFilter.class,
    GlobalExceptionHandler.class,
    ApiSurfaceAndErrorHandlingTest.TestProbeController.class
})
class ApiSurfaceAndErrorHandlingTest {

    @Autowired private MockMvc mockMvc;

    @RestController
    static class TestProbeController {

        record ProbeInput(@NotBlank String name) {}

        @PostMapping("/api/test/validation")
        public void validateInput(@Valid @RequestBody ProbeInput input) {}

        @GetMapping("/api/test/domain-error")
        public void throwDomainError() {
            throw DomainError.conflict(
                    "booking.room_not_available", "Selected room is not available.");
        }

        @GetMapping("/api/test/domain-not-found")
        public void throwDomainNotFound() {
            throw DomainError.notFound(
                    "booking.reservation_not_found", "Reservation was not found.");
        }

        @GetMapping("/api/test/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access is denied for the current principal.");
        }

        @GetMapping("/api/test/response-status")
        public void throwResponseStatus() {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable");
        }

        @GetMapping("/api/test/internal-error")
        public void throwUnexpectedError() {
            throw new IllegalStateException(
                    "Sensitive internal database detail in "
                            + TestProbeController.class.getName());
        }

        @GetMapping("/api/test/mdc")
        public String currentMdcRequestId() {
            return MDC.get(RequestIdFilter.MDC_KEY);
        }

        @GetMapping("/admin-api/test/protected")
        public String adminProtected() {
            return "should-not-reach";
        }
    }

    @Test
    void shouldReturnEmptyItemsList_whenCallingPublicSampleItemsScaffoldEndpoint()
            throws Exception {
        mockMvc.perform(get("/api/system/sample-items"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldReturn401ProblemDetail_whenAccessingAdminApiWithoutAuthentication()
            throws Exception {
        mockMvc.perform(
                        get("/admin-api/test/protected")
                                .header("X-Request-Id", "req-admin-check-01"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Request-Id", "req-admin-check-01"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/admin-api/test/protected"))
                .andExpect(jsonPath("$.code").value("security.unauthorized"))
                .andExpect(jsonPath("$.requestId").value("req-admin-check-01"));
    }

    @Test
    void shouldReturn401ProblemDetail_whenCallingUnknownPublicPathOutsideAllowlist()
            throws Exception {
        mockMvc.perform(get("/unknown-public-path"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("security.unauthorized"))
                .andExpect(jsonPath("$.instance").value("/unknown-public-path"));
    }

    @Test
    void shouldReturnProblemDetailFromErrorDispatch_whenContainerForwardsToErrorEndpoint()
            throws Exception {
        mockMvc.perform(
                        get("/error")
                                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                                .requestAttr(
                                        RequestDispatcher.ERROR_REQUEST_URI,
                                        "/some/unknown/resource"))
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/some/unknown/resource"))
                .andExpect(jsonPath("$.code").value("system.not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void shouldAcceptValidRequestIdAndPropagateToMdcAndResponse_whenHeaderMatchesPattern()
            throws Exception {
        String validId = "client_trace-2026.10.18";

        mockMvc.perform(get("/api/test/mdc").header("X-Request-Id", validId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", validId))
                .andExpect(content().string(validId));
    }

    @Test
    void shouldGenerateUuidV7RequestId_whenHeaderIsMissingOrInvalid() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/api/system/sample-items")
                                        .header("X-Request-Id", "bad!id"))
                        .andExpect(status().isOk())
                        .andReturn();

        String generated = result.getResponse().getHeader("X-Request-Id");
        assertThat(generated)
                .isNotNull()
                .isNotEqualTo("bad!id")
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    @Test
    void shouldReturnStandardProblemDetailEnvelope_whenValidation400NotFound404Method405Occur()
            throws Exception {
        // 400 -> validation.invalid_request
        mockMvc.perform(
                        post("/api/test/validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("validation.invalid_request"))
                .andExpect(jsonPath("$.instance").value("/api/test/validation"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        // 404 -> system.not_found
        mockMvc.perform(get("/api/non-existent-resource"))
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("system.not_found"))
                .andExpect(jsonPath("$.instance").value("/api/non-existent-resource"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        // 405 -> system.method_not_allowed
        mockMvc.perform(post("/api/system/sample-items"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("system.method_not_allowed"))
                .andExpect(jsonPath("$.instance").value("/api/system/sample-items"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void shouldPreserveClientErrorStatuses_when415And406Occur() throws Exception {
        // 415 -> system.unsupported_media_type
        mockMvc.perform(
                        post("/api/test/validation")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("system.unsupported_media_type"));

        // 406 -> system.not_acceptable (client only accepts XML, endpoint produces JSON)
        mockMvc.perform(get("/api/system/sample-items").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.code").value("system.not_acceptable"));
    }

    @Test
    void shouldReturn403Forbidden_whenAccessDeniedExceptionThrown() throws Exception {
        mockMvc.perform(get("/api/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("security.forbidden"))
                .andExpect(jsonPath("$.instance").value("/api/test/access-denied"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void shouldPreserveResponseStatusExceptionStatus_whenUnprocessableEntityThrown()
            throws Exception {
        mockMvc.perform(get("/api/test/response-status"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("system.http_422"));
    }

    @Test
    void shouldMapDomainErrorCategoriesToHttpStatuses_whenConflictAndNotFoundThrown()
            throws Exception {
        // CONFLICT -> 409
        mockMvc.perform(get("/api/test/domain-error"))
                .andExpect(status().isConflict())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.code").value("booking.room_not_available"))
                .andExpect(jsonPath("$.detail").value("Selected room is not available."));

        // NOT_FOUND -> 404
        mockMvc.perform(get("/api/test/domain-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("booking.reservation_not_found"));
    }

    @Test
    void shouldReturn500ProblemDetailWithoutLeakingDetails_whenUnexpectedExceptionOccurs()
            throws Exception {
        MvcResult result500 =
                mockMvc.perform(get("/api/test/internal-error"))
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
                .doesNotContain("TestProbeController")
                .doesNotContain("Sensitive internal database detail");
    }

    @Test
    void shouldAllowWebOriginAndExposeRequestIdWhileRejectingDisallowedOrigin_whenCheckingCors()
            throws Exception {
        // Allowed origin: http://localhost:3000
        mockMvc.perform(
                        options("/api/system/sample-items")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(
                        header().string("Access-Control-Expose-Headers", "X-Request-Id"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

        // Negative test: disallowed origin must not receive Access-Control-Allow-Origin
        mockMvc.perform(
                        get("/api/system/sample-items")
                                .header("Origin", "http://localhost:3001"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
