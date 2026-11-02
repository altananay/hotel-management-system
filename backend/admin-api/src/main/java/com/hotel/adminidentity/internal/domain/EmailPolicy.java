package com.hotel.adminidentity.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Email format and normalization (spec 0004 §2.2.1). */
public final class EmailPolicy {

    public static final int MAX_LENGTH = 254;
    public static final int MAX_LOCAL_PART_LENGTH = 64;

    private static final Pattern PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                            + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$");

    private EmailPolicy() {}

    public static String normalizeAndValidate(String rawEmail) {
        if (rawEmail == null) {
            throw new InvalidIdentityException("Email is required.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        int at = email.indexOf('@');
        if (email.length() > MAX_LENGTH
                || at <= 0
                || at != email.lastIndexOf('@')
                || at > MAX_LOCAL_PART_LENGTH
                || !PATTERN.matcher(email).matches()) {
            throw new InvalidIdentityException("Email format is invalid.");
        }
        return email;
    }
}
