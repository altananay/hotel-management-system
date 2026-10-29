package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.adminidentity.internal.domain.SessionTokenHasher;
import org.junit.jupiter.api.Test;

class SessionTokenHasherTest {

    private final SessionTokenHasher hasher = new SessionTokenHasher();

    @Test
    void shouldProduceUrlSafeTokenAndStableHash_whenGeneratingSessionToken() {
        String token = hasher.newToken();
        String hash = hasher.hash(token);

        assertThat(token).hasSize(43).doesNotContain("=");
        assertThat(hash).hasSize(64);
        assertThat(hasher.matches(token, hash)).isTrue();
        assertThat(hasher.matches(token + "x", hash)).isFalse();
    }
}
