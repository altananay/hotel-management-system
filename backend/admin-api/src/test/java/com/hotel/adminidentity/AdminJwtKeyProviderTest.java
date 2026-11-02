package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.adminidentity.internal.application.AdminJwtKeyProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdminJwtKeyProviderTest {

    @TempDir Path tempDir;

    @Test
    void shouldPublishActiveAndPreviousKeys_whenPreviousPublicKeyExists() throws Exception {
        KeyPair active = keyPair();
        KeyPair previous = keyPair();
        Path activePath = tempDir.resolve("admin-jwt-active.pem");
        Path previousPath = tempDir.resolve("admin-jwt-previous-public.pem");
        writePrivateKey(activePath, active.getPrivate());
        writePublicKey(previousPath, previous.getPublic());
        AdminJwtKeyProvider provider =
                new AdminJwtKeyProvider(activePath.toString(), previousPath.toString(), false);

        provider.initialize();

        assertThat(provider.publicJwks().getKeys()).hasSize(2);
        assertThat(provider.activeKid()).isNotBlank();
        assertThat(provider.activeKid()).isEqualTo(provider.activeKey().getKeyID());
        assertThat(provider.publicJwks().getKeys())
                .extracting(key -> key.getKeyID())
                .contains(provider.activeKid());
    }

    @Test
    void shouldPublishOnlyActiveKey_whenPreviousKeyIsAbsent() throws Exception {
        KeyPair active = keyPair();
        Path activePath = tempDir.resolve("admin-jwt-active.pem");
        writePrivateKey(activePath, active.getPrivate());
        AdminJwtKeyProvider provider =
                new AdminJwtKeyProvider(activePath.toString(), "", false);

        provider.initialize();

        assertThat(provider.publicJwks().getKeys()).hasSize(1);
    }

    @Test
    void shouldKeepSameKeyIdAcrossRestart_whenActiveKeyFileIsReused() throws Exception {
        KeyPair active = keyPair();
        Path activePath = tempDir.resolve("admin-jwt-active.pem");
        writePrivateKey(activePath, active.getPrivate());

        AdminJwtKeyProvider first =
                new AdminJwtKeyProvider(activePath.toString(), "", false);
        first.initialize();
        String firstKid = first.activeKid();

        AdminJwtKeyProvider restarted =
                new AdminJwtKeyProvider(activePath.toString(), "", false);
        restarted.initialize();

        assertThat(restarted.activeKid()).isEqualTo(firstKid);
    }

    @Test
    void shouldFailFast_whenActiveKeyIsMissingAndGenerationIsNotAllowed() {
        AdminJwtKeyProvider provider =
                new AdminJwtKeyProvider(
                        tempDir.resolve("missing.pem").toString(), "", false);

        assertThatThrownBy(provider::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Active admin JWT private key is missing");
    }

    @Test
    void shouldGenerateLocalKeyPair_whenGenerationIsAllowed() {
        Path activePath = tempDir.resolve("generated/admin-jwt-active.pem");
        AdminJwtKeyProvider provider =
                new AdminJwtKeyProvider(activePath.toString(), "", true);

        provider.initialize();

        assertThat(activePath).exists();
        assertThat(tempDir.resolve("generated/admin-jwt-active-public.pem")).exists();
        assertThat(provider.activeKid()).isNotBlank();
        assertThat(provider.publicJwks().getKeys()).hasSize(1);
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void writePrivateKey(Path path, PrivateKey key) throws IOException {
        writePem(path, "PRIVATE KEY", key.getEncoded());
    }

    private void writePublicKey(Path path, PublicKey key) throws IOException {
        writePem(path, "PUBLIC KEY", key.getEncoded());
    }

    private void writePem(Path path, String type, byte[] der) throws IOException {
        String encoded =
                Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(der);
        Files.writeString(
                path,
                "-----BEGIN " + type + "-----\n"
                        + encoded
                        + "\n-----END "
                        + type
                        + "-----\n",
                StandardCharsets.UTF_8);
    }
}
