package com.hotel.gateway.internal.application;

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
    private final HttpClient mediaHttpClient;
    private final String upstreamBaseUrl;
    private final Duration timeout;
    private final Duration mediaTimeout;

    public AdminGatewayClient(
            @Value("${admin.gateway.upstream-hotel-api-url:http://localhost:8081}")
                    String upstreamBaseUrl,
            @Value("${admin.gateway.timeout:3s}") Duration timeout,
            @Value("${admin.gateway.media-timeout:15s}") Duration mediaTimeout) {
        this.upstreamBaseUrl = upstreamBaseUrl.replaceAll("/$", "");
        this.timeout = timeout;
        this.mediaTimeout = mediaTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.mediaHttpClient = HttpClient.newBuilder().connectTimeout(mediaTimeout).build();
    }

    public GatewayResponse exchange(
            HttpServletRequest request, byte[] body, String jwt, String requestId) {
        URI uri = URI.create(upstreamBaseUrl + request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(uri)
                        .timeout(requestTimeout(request.getRequestURI()))
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
                    clientFor(request.getRequestURI())
                            .send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
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

    /**
     * Media management performs synchronous S3 copy/delete and full-object validation; those
     * operations need a longer budget than the general admin gateway timeout (R-01). The
     * connect timeout must grow with the request timeout: a slow connection would otherwise
     * fail media calls before the 15-second request budget can be used.
     */
    private Duration requestTimeout(String requestUri) {
        return isMediaRequest(requestUri) ? mediaTimeout : timeout;
    }

    HttpClient clientFor(String requestUri) {
        return isMediaRequest(requestUri) ? mediaHttpClient : httpClient;
    }

    static boolean isMediaRequest(String requestUri) {
        return requestUri.equals("/admin-api/media") || requestUri.startsWith("/admin-api/media/");
    }
}
