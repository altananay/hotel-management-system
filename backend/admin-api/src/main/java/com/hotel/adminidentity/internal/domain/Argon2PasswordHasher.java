package com.hotel.adminidentity.internal.domain;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher {

    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    private final String dummyHash = encoder.encode("dummy-password-for-timing");

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedHash) {
        return encodedHash != null && encoder.matches(rawPassword, encodedHash);
    }

    public String dummyHash() {
        return dummyHash;
    }
}
