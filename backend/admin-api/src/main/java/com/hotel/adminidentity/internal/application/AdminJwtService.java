package com.hotel.adminidentity.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminidentity.api.AdminIdentityApi.AdminSessionView;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminJwtService {

    private final AdminJwtKeyProvider keyProvider;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration ttl;

    public AdminJwtService(
            AdminJwtKeyProvider keyProvider,
            Clock clock,
            @Value("${admin.jwt.issuer:admin-api}") String issuer,
            @Value("${admin.jwt.audience:hotel-admin-api}") String audience,
            @Value("${admin.jwt.ttl:5m}") Duration ttl) {
        this.keyProvider = keyProvider;
        this.clock = clock;
        this.issuer = issuer;
        this.audience = audience;
        this.ttl = ttl;
    }

    public String issue(AdminSessionView session) {
        Instant now = clock.instant();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(audience)
                        .subject(session.userId().toString())
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plus(ttl)))
                        .jwtID(Generators.timeBasedEpochGenerator().generate().toString())
                        .claim("roles", List.of(session.role()))
                        .build();
        JWSHeader header =
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(keyProvider.activeKid())
                        .type(com.nimbusds.jose.JOSEObjectType.JWT)
                        .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new RSASSASigner(keyProvider.activeKey()));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Could not sign admin JWT.", ex);
        }
        return jwt.serialize();
    }
}
