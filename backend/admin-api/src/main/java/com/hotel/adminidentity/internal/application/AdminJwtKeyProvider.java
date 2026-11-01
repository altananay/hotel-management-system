package com.hotel.adminidentity.internal.application;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminJwtKeyProvider {

    private final Path activeKeyPath;
    private final Path previousPublicKeyPath;
    private final boolean allowGeneratedDevKey;

    private RSAKey activeKey;
    private RSAKey previousPublicKey;

    public AdminJwtKeyProvider(
            @Value("${admin.jwt.active-key-path:.local/admin-jwt-active.pem}")
                    String activeKeyPath,
            @Value("${admin.jwt.previous-public-key-path:}") String previousPublicKeyPath,
            @Value("${admin.jwt.allow-generated-dev-key:false}") boolean allowGeneratedDevKey) {
        this.activeKeyPath = Path.of(activeKeyPath);
        this.previousPublicKeyPath =
                previousPublicKeyPath == null || previousPublicKeyPath.isBlank()
                        ? null
                        : Path.of(previousPublicKeyPath);
        this.allowGeneratedDevKey = allowGeneratedDevKey;
    }

    @PostConstruct
    public void initialize() {
        if (!Files.exists(activeKeyPath)) {
            if (!allowGeneratedDevKey) {
                throw new IllegalStateException(
                        "Active admin JWT private key is missing: " + activeKeyPath);
            }
            generateDevKey();
        }
        PrivateKey privateKey = readPrivateKey(activeKeyPath);
        PublicKey publicKey = derivePublicKey(privateKey);
        activeKey = toRsaKey(publicKey, privateKey);
        if (previousPublicKeyPath != null && Files.exists(previousPublicKeyPath)) {
            previousPublicKey = toPublicRsaKey(readPublicKey(previousPublicKeyPath));
        }
    }

    public RSAKey activeKey() {
        return activeKey;
    }

    public String activeKid() {
        return activeKey.getKeyID();
    }

    public JWKSet publicJwks() {
        List<com.nimbusds.jose.jwk.JWK> keys = new ArrayList<>();
        keys.add(activeKey.toPublicJWK());
        if (previousPublicKey != null) {
            keys.add(previousPublicKey);
        }
        return new JWKSet(keys);
    }

    private void generateDevKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Path parent = activeKeyPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(activeKeyPath, toPem("PRIVATE KEY", pair.getPrivate().getEncoded()));
            Files.writeString(
                    siblingPublicPath(), toPem("PUBLIC KEY", pair.getPublic().getEncoded()));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate local admin JWT key.", ex);
        }
    }

    private Path siblingPublicPath() {
        String name = activeKeyPath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String publicName = (dot > 0 ? name.substring(0, dot) : name) + "-public.pem";
        return activeKeyPath.resolveSibling(publicName);
    }

    private PrivateKey readPrivateKey(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.UTF_8);
            byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read admin JWT private key: " + path, ex);
        }
    }

    private PublicKey readPublicKey(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.UTF_8);
            byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read admin JWT public key: " + path, ex);
        }
    }

    private PublicKey derivePublicKey(PrivateKey privateKey) {
        try {
            RSAPrivateCrtKey rsaPrivate = (RSAPrivateCrtKey) privateKey;
            java.security.spec.RSAPublicKeySpec spec =
                    new java.security.spec.RSAPublicKeySpec(
                            rsaPrivate.getModulus(), rsaPrivate.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not derive admin JWT public key.", ex);
        }
    }

    private RSAKey toRsaKey(PublicKey publicKey, PrivateKey privateKey) {
        try {
            RSAKey key =
                    new RSAKey.Builder((RSAPublicKey) publicKey)
                            .privateKey(privateKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .build();
            return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("Could not build admin JWT key.", ex);
        }
    }

    private RSAKey toPublicRsaKey(PublicKey publicKey) {
        try {
            RSAKey key =
                    new RSAKey.Builder((RSAPublicKey) publicKey)
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .build();
            return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
        } catch (com.nimbusds.jose.JOSEException ex) {
            throw new IllegalStateException("Could not build admin JWT public key.", ex);
        }
    }

    private String stripPem(String pem) {
        return pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private String toPem(String type, byte[] der) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + encoded + "\n-----END " + type + "-----\n";
    }
}
