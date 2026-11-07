package com.hotel.gateway.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.gateway.api.GatewayTimeoutException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AdminGatewayClientTest {

    private HttpServer server;
    private AdminGatewayClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client =
                new AdminGatewayClient(
                        "http://localhost:" + server.getAddress().getPort(),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldForwardOnlyAllowedHeadersAndUseGatewayRequestId() throws Exception {
        AtomicReference<Map<String, java.util.List<String>>> captured = new AtomicReference<>();
        server.createContext(
                "/admin-api/test",
                exchange -> {
                    captured.set(exchange.getRequestHeaders());
                    byte[] body = "upstream".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/test");
        request.setQueryString("q=1");
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Cookie", "ADMIN_SESSION=secret");
        request.addHeader("Authorization", "Bearer client-token");
        request.addHeader("X-Request-Id", "client-request-id");
        request.addHeader("Origin", "http://localhost:3001");
        request.addHeader("Referer", "http://localhost:3001/admin");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader("X-Internal-User", "spoofed");

        GatewayResponse response =
                client.exchange(
                        request,
                        "{}".getBytes(StandardCharsets.UTF_8),
                        "gateway-jwt",
                        "edge-request-id");

        assertThat(response.status()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("upstream");
        assertThat(captured.get().get("Authorization")).containsExactly("Bearer gateway-jwt");
        assertThat(captured.get().get("Cookie")).isNull();
        assertThat(captured.get().get("Origin")).isNull();
        assertThat(captured.get().get("Referer")).isNull();
        assertThat(captured.get().get("X-Forwarded-For")).isNull();
        assertThat(captured.get().get("X-Internal-User")).isNull();
        assertThat(captured.get().get("X-Request-Id"))
                .containsExactly("edge-request-id")
                .doesNotContain("client-request-id");
        assertThat(response.requestId()).isEqualTo("edge-request-id");
    }

    @Test
    void shouldMapConnectionRefusedToGatewayUnavailable() {
        AdminGatewayClient unavailableClient =
                new AdminGatewayClient(
                        "http://127.0.0.1:1", Duration.ofMillis(200), Duration.ofSeconds(5));

        assertThatThrownBy(
                        () ->
                                unavailableClient.exchange(
                                        new MockHttpServletRequest("GET", "/admin-api/rooms"),
                                        null,
                                        "jwt",
                                        "edge-request-id"))
                .isInstanceOf(com.hotel.gateway.api.GatewayUnavailableException.class);
    }

    @Test
    void shouldMapSlowUpstreamToGatewayTimeout() throws Exception {
        server.createContext(
                "/admin-api/slow",
                exchange -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                });
        AdminGatewayClient shortTimeoutClient =
                new AdminGatewayClient(
                        "http://localhost:" + server.getAddress().getPort(),
                        Duration.ofMillis(50),
                        Duration.ofSeconds(5));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/slow");

        assertThatThrownBy(
                        () ->
                                shortTimeoutClient.exchange(
                                        request, null, "jwt", "edge-request-id"))
                .isInstanceOf(GatewayTimeoutException.class);
    }

    @Test
    void shouldUseMediaSpecificTimeoutForMediaRequests() throws Exception {
        server.createContext(
                "/admin-api/media/slow",
                exchange -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    byte[] body = "media-ok".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        AdminGatewayClient mediaTimeoutClient =
                new AdminGatewayClient(
                        "http://localhost:" + server.getAddress().getPort(),
                        Duration.ofMillis(50),
                        Duration.ofMillis(1000));

        GatewayResponse response =
                mediaTimeoutClient.exchange(
                        new MockHttpServletRequest("GET", "/admin-api/media/slow"),
                        null,
                        "jwt",
                        "edge-request-id");

        assertThat(response.status()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("media-ok");
    }

    @Test
    void shouldUseMediaSpecificConnectTimeoutForMediaRequests() {
        assertThat(client.clientFor("/admin-api/media/1").connectTimeout())
                .contains(Duration.ofSeconds(5));
        assertThat(client.clientFor("/admin-api/rooms").connectTimeout())
                .contains(Duration.ofMillis(500));
    }

    @Test
    void shouldTreatOnlyMediaPathsAsMediaRequests() {
        assertThat(AdminGatewayClient.isMediaRequest("/admin-api/media")).isTrue();
        assertThat(AdminGatewayClient.isMediaRequest("/admin-api/media/1/complete")).isTrue();
        assertThat(AdminGatewayClient.isMediaRequest("/admin-api/mediafoo")).isFalse();
        assertThat(AdminGatewayClient.isMediaRequest("/admin-api/rooms")).isFalse();
    }
}
