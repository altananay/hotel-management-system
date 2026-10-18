package com.hotel.sharedkernel.api;

import java.util.Objects;
import java.util.regex.Pattern;

public class DomainError extends RuntimeException {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*\\.[a-z0-9_]+$");

    private final String code;

    public DomainError(String code, String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        Objects.requireNonNull(code, "code must not be null");
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Invalid domain error code format '" + code + "'; expected <module>.<snake_case>");
        }
        this.code = code;
    }

    public DomainError(String code, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        Objects.requireNonNull(code, "code must not be null");
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Invalid domain error code format '" + code + "'; expected <module>.<snake_case>");
        }
        this.code = code;
    }

    public String code() {
        return code;
    }
}
