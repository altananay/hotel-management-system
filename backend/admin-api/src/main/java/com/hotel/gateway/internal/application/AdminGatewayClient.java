package com.hotel.gateway.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.gateway.api.GatewayTimeoutException;
import com.hotel.gateway.api.GatewayUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminGatewayClient {

    private final HttpClient httpClient;
    private final String upstreamBaseUrl;
    private final Duration timeout;

    public AdminGatewayClient(
            @Value("${admin.gateway.upstream-hotel-api-url:http://localhost:8081}")
                    String upstreamBaseUrl,
            @Value("${admin.gateway.timeout:3s}") Duration timeout) {
        this.upstreamBaseUrl = upstreamBaseUrl.replaceAll("/$", "");
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public GatewayResponse exchange(
            HttpServletRequest request, byte[] body, String jwt) {
        String requestId = Generators.timeBasedEpochGenerator().generate().toString();
        URI uri = URI.create(upstreamBaseUrl + request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri)
                        .timeout(timeout)
                        .header("Authorization", "Bearer " + jwt)
                        .header("X-Request-Id", requestId);
        if (request.getHeader("Accept") != null) {
            builder.header("Accept", request.getHeader("Accept"));
        }
        if (request.getHeader("Content-Type") != null) {
            builder.header("Content-Type", request.getHeader("Content-Type"));
        }
        HttpRequest.BodyPublisher publisher =
                body == null || body.length == 0
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body);
        builder.method(request.getMethod(), publisher);

        try {
            HttpResponse<byte[]> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String contentType =
                    response.headers().firstValue("Content-Type").orElse("application/octet-stream");
            return new GatewayResponse(
                    response.statusCode(), contentType, response.body(), requestId);
        } catch (HttpTimeoutException ex) {
            throw new GatewayTimeoutException();
        } catch (ConnectException ex) {
            throw new GatewayUnavailableException(ex);
        } catch (IOException ex) {
            throw new GatewayUnavailableException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GatewayUnavailableException(ex);
        }
    }
}
