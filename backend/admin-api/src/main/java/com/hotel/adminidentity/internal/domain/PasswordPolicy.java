package com.hotel.adminidentity.internal.domain;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public final class PasswordPolicy {

    public static final int MIN_CODE_POINTS = 12;
    public static final int MAX_CODE_POINTS = 128;
    public static final int MAX_UTF8_BYTES = 512;

    private PasswordPolicy() {}

    public static String validate(String rawPassword, String username, String email) {
        if (rawPassword == null) {
            throw new InvalidPasswordException("Password is required.");
        }
        String password = Normalizer.normalize(rawPassword, Normalizer.Form.NFC);
        int codePoints = password.codePointCount(0, password.length());
        if (codePoints < MIN_CODE_POINTS || codePoints > MAX_CODE_POINTS) {
            throw new InvalidPasswordException("Password length is invalid.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new InvalidPasswordException("Password is too long.");
        }
        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        if (username != null
                && !username.isBlank()
                && normalizedPassword.contains(username.toLowerCase(Locale.ROOT))) {
            throw new InvalidPasswordException("Password contains the username.");
        }
        if (email != null && !email.isBlank()) {
            String normalizedEmail = email.toLowerCase(Locale.ROOT);
            String localPart = normalizedEmail.contains("@")
                    ? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
                    : normalizedEmail;
            if (normalizedPassword.contains(normalizedEmail)
                    || (localPart.length() >= 3 && normalizedPassword.contains(localPart))) {
                throw new InvalidPasswordException("Password contains the email.");
            }
        }
        return password;
    }
}
