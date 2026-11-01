package com.hotel.adminidentity.internal.domain;

import java.text.Normalizer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher {

    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    private final String dummyHash = encoder.encode("dummy-password-for-timing");

    public String hash(String rawPassword) {
        return encoder.encode(normalize(rawPassword));
    }

    public boolean matches(String rawPassword, String encodedHash) {
        return encodedHash != null && encoder.matches(normalize(rawPassword), encodedHash);
    }

    private String normalize(String rawPassword) {
        return rawPassword == null
                ? null
                : Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
    }

    public String dummyHash() {
        return dummyHash;
    }
}
