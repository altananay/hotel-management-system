package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.adminidentity.internal.domain.Argon2PasswordHasher;
import org.junit.jupiter.api.Test;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void shouldHashWithArgon2idParameters_whenPasswordIsHashed() {
        String hash = hasher.hash("Kapadokya-Guvenli-2026!");

        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(hash).doesNotContain("Kapadokya-Guvenli-2026!");
        assertThat(hasher.matches("Kapadokya-Guvenli-2026!", hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }

    @Test
    void shouldKeepDummyHashAvailable_whenUnknownUserIsChecked() {
        assertThat(hasher.dummyHash()).startsWith("$argon2id$");
        assertThat(hasher.matches("anything", hasher.dummyHash())).isFalse();
    }
}
