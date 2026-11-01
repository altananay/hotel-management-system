package com.hotel.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class AdminJwtConfig {

    private static final int JWKS_SIZE_LIMIT_BYTES = 512 * 1024;

    @Bean
    public JwtDecoder adminJwtDecoder(
            @Value("${hotel.admin.jwks-uri}") String jwksUri,
            @Value("${hotel.admin.issuer:admin-api}") String issuer,
            @Value("${hotel.admin.audience:hotel-admin-api}") String audience,
            @Value("${hotel.admin.jwks-cache-ttl:5m}") Duration cacheTtl,
            @Value("${hotel.admin.jwks-cache-refresh:15s}") Duration cacheRefresh,
            @Value("${hotel.admin.jwks-cooldown:60s}") Duration cooldown,
            @Value("${hotel.admin.jwks-connect-timeout:2s}") Duration connectTimeout,
            @Value("${hotel.admin.jwks-read-timeout:2s}") Duration readTimeout,
            Clock clock)
            throws MalformedURLException {
        ResourceRetriever retriever =
                new DefaultResourceRetriever(
                        Math.toIntExact(connectTimeout.toMillis()),
                        Math.toIntExact(readTimeout.toMillis()),
                        JWKS_SIZE_LIMIT_BYTES);
        JWKSource<SecurityContext> jwkSource =
                JWKSourceBuilder.<SecurityContext>create(
                                URI.create(jwksUri).toURL(), retriever)
                        .cache(cacheTtl.toMillis(), cacheRefresh.toMillis())
                        .refreshAheadCache(false)
                        .rateLimited(cooldown.toMillis())
                        .build();
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSource(jwkSource)
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        values -> values != null && values.contains(audience));
        OAuth2TokenValidator<Jwt> issuedAtValidator =
                new JwtClaimValidator<Instant>(
                        JwtClaimNames.IAT,
                        issuedAt ->
                                issuedAt == null
                                        || !issuedAt.isAfter(clock.instant().plusSeconds(60)));
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        audienceValidator,
                        issuedAtValidator));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter adminJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }
}
