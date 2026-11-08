package com.hotel.pricing.internal.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class HttpTcmbFxClient implements TcmbFxClient {

    private final HttpClient httpClient;
    private final String url;
    private final Duration readTimeout;

    public HttpTcmbFxClient(PricingProperties properties) {
        this.url = properties.getTcmb().getUrl();
        this.readTimeout = properties.getTcmb().getReadTimeout();
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(properties.getTcmb().getConnectTimeout())
                        .build();
    }

    @Override
    public String fetchTodayXml() {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(readTimeout)
                            .GET()
                            .build();
            HttpResponse<String> response =
                    httpClient.send(
                            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new TcmbUnavailableException(
                        "TCMB returned HTTP " + response.statusCode(), null);
            }
            return response.body();
        } catch (TcmbUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TcmbUnavailableException("TCMB could not be reached.", ex);
        }
    }
}
