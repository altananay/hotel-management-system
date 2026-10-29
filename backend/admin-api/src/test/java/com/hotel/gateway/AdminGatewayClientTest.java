package com.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.gateway.api.GatewayTimeoutException;
import com.hotel.gateway.internal.application.AdminGatewayClient;
import com.hotel.gateway.internal.application.GatewayResponse;
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
                        Duration.ofMillis(500));
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

        GatewayResponse response =
                client.exchange(request, "{}".getBytes(StandardCharsets.UTF_8), "gateway-jwt");

        assertThat(response.status()).isEqualTo(200);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("upstream");
        assertThat(captured.get().get("Authorization")).containsExactly("Bearer gateway-jwt");
        assertThat(captured.get().get("Cookie")).isNull();
        assertThat(captured.get().get("X-Request-Id"))
                .isNotEmpty()
                .doesNotContain("client-request-id");
        assertThat(response.requestId()).isNotBlank();
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
                        Duration.ofMillis(50));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/slow");

        assertThatThrownBy(() -> shortTimeoutClient.exchange(request, null, "jwt"))
                .isInstanceOf(GatewayTimeoutException.class);
    }
}
