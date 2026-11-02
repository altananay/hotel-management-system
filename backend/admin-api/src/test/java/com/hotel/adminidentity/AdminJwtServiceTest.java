package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.hotel.adminidentity.internal.application.AdminJwtKeyProvider;
import com.hotel.adminidentity.internal.application.AdminJwtService;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminJwtServiceTest {

    @TempDir Path tempDir;

    @Test
    void shouldIssueFiveMinuteRs256JwtWithRequiredClaims_whenSessionIsValid() throws Exception {
        AdminJwtKeyProvider keyProvider =
                new AdminJwtKeyProvider(tempDir.resolve("active.pem").toString(), "", true);
        keyProvider.initialize();
        AdminJwtService service =
                new AdminJwtService(
                        keyProvider,
                        Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC),
                        "admin-api",
                        "hotel-admin-api",
                        Duration.ofMinutes(5));

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String token =
                service.issue(
                        new AdminSessionView(
                                userId, sessionId, "superadmin", "superadmin@example.com", "SUPER_ADMIN"));

        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(keyProvider.activeKid());
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("admin-api");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("hotel-admin-api");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("roles"))
                .containsExactly("SUPER_ADMIN");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant())
                .isEqualTo(Instant.parse("2026-10-29T10:05:00Z"));
        assertThat(jwt.verify(new RSASSAVerifier(keyProvider.activeKey().toRSAPublicKey())))
                .isTrue();
        assertThat(keyProvider.publicJwks().getKeys()).hasSize(1);
        assertThat(keyProvider.publicJwks().getKeys().getFirst().isPrivate()).isFalse();
    }
}
