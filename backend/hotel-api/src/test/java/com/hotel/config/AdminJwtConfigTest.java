package com.hotel.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class AdminJwtConfigTest {

    private HttpServer server;
    private final AtomicReference<JWKSet> servedKeys = new AtomicReference<>();
    private final AtomicInteger fetchCount = new AtomicInteger();

    @BeforeEach
    void startJwksServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/jwks",
                exchange -> {
                    fetchCount.incrementAndGet();
                    byte[] body =
                            servedKeys.get().toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopJwksServer() {
        server.stop(0);
    }

    @Test
    void shouldCacheJwksAndRefreshOnce_whenUnknownKidAppearsAfterCooldown() throws Exception {
        RSAKey first = key();
        servedKeys.set(new JWKSet(List.of(first.toPublicJWK())));
        JwtDecoder decoder = decoder(Duration.ofMillis(20), jwksUrl());
        Instant now = Instant.now();

        assertThat(decoder.decode(token(first, now, now.plusSeconds(300)))).isNotNull();
        assertThat(fetchCount).hasValue(1);
        assertThat(decoder.decode(token(first, now, now.plusSeconds(300)))).isNotNull();
        assertThat(fetchCount).hasValue(1);

        RSAKey second = key();
        servedKeys.set(new JWKSet(List.of(first.toPublicJWK(), second.toPublicJWK())));
        Thread.sleep(50);

        assertThat(decoder.decode(token(second, now, now.plusSeconds(300)))).isNotNull();
        assertThat(fetchCount).hasValue(2);
    }

    @Test
    void shouldLimitUnknownKidRefreshToOnePerCooldown() throws Exception {
        RSAKey first = key();
        servedKeys.set(new JWKSet(List.of(first.toPublicJWK())));
        JwtDecoder decoder = decoder(Duration.ofSeconds(60), jwksUrl());
        Instant now = Instant.now();
        decoder.decode(token(first, now, now.plusSeconds(300)));
        assertThat(fetchCount).hasValue(1);

        RSAKey secondUnknown = key();
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        token(secondUnknown, now, now.plusSeconds(300))))
                .isInstanceOf(JwtException.class);
        assertThat(fetchCount).hasValue(2);

        RSAKey thirdUnknown = key();
        assertThatThrownBy(
                        () -> decoder.decode(token(thirdUnknown, now, now.plusSeconds(300))))
                .isInstanceOf(JwtException.class);
        assertThat(fetchCount).hasValue(2);
    }

    @Test
    void shouldReturn401StyleValidationError_whenIssuedAtIsMoreThanSixtySecondsInFuture()
            throws Exception {
        RSAKey key = key();
        servedKeys.set(new JWKSet(List.of(key.toPublicJWK())));
        JwtDecoder decoder = decoder(Duration.ofMillis(20), jwksUrl());
        Instant now = Instant.now();

        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        token(key, now.plusSeconds(120), now.plusSeconds(300))))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldFailWhenJwksCannotBeRetrievedAndCacheIsEmpty() throws Exception {
        RSAKey key = key();
        JwtDecoder decoder =
                decoder(Duration.ofMillis(20), "http://127.0.0.1:1/jwks");
        Instant now = Instant.now();

        assertThatThrownBy(() -> decoder.decode(token(key, now, now.plusSeconds(300))))
                .isInstanceOf(JwtException.class)
                .hasRootCauseInstanceOf(IOException.class);
    }

    private JwtDecoder decoder(Duration cooldown, String jwksUrl) throws MalformedURLException {
        return new AdminJwtConfig()
                .adminJwtDecoder(
                        jwksUrl,
                        "admin-api",
                        "hotel-admin-api",
                        Duration.ofMinutes(2),
                        Duration.ofSeconds(5),
                        cooldown,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Clock.systemUTC());
    }

    private String jwksUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    private String token(RSAKey key, Instant issuedAt, Instant expiresAt) throws Exception {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer("admin-api")
                        .audience("hotel-admin-api")
                        .subject(UUID.randomUUID().toString())
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiresAt))
                        .jwtID(UUID.randomUUID().toString())
                        .claim("roles", List.of("SUPER_ADMIN"))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(key.getKeyID())
                                .build(),
                        claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private RSAKey key() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAKey key =
                new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                        .privateKey(pair.getPrivate())
                        .keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256)
                        .build();
        return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
    }
}
