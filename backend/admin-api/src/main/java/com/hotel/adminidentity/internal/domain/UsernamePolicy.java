package com.hotel.adminidentity.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Username format and normalization (spec 0004 §2.2.1). */
public final class UsernamePolicy {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,39}$");

    private UsernamePolicy() {}

    public static String normalizeAndValidate(String rawUsername) {
        if (rawUsername == null) {
            throw new InvalidIdentityException("Username is required.");
        }
        String username = rawUsername.trim().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(username).matches()) {
            throw new InvalidIdentityException("Username format is invalid.");
        }
        return username;
    }
}
