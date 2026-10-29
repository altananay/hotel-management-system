package com.hotel.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@TestConfiguration
@EnableMethodSecurity
public class AdminJwtTestConfiguration {

    private static final RSAKey RSA_KEY = generateKey();

    @Bean
    JwtDecoder adminJwtDecoder() {
        try {
            NimbusJwtDecoder decoder =
                    NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
            OAuth2TokenValidator<Jwt> audience =
                    new JwtClaimValidator<List<String>>(
                            JwtClaimNames.AUD,
                            values -> values != null && values.contains("hotel-admin-api"));
            decoder.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(
                            JwtValidators.createDefaultWithIssuer("admin-api"), audience));
            return decoder;
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    JwtAuthenticationConverter adminJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }

    public static RSAKey rsaKey() {
        return RSA_KEY;
    }

    private static RSAKey generateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            RSAKey key =
                    new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                            .privateKey(pair.getPrivate())
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .build();
            try {
                return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
            } catch (com.nimbusds.jose.JOSEException ex) {
                throw new IllegalStateException(ex);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate test JWT key.", ex);
        }
    }
}
