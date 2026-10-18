package com.hotel.sharedkernel.api;

import java.util.Objects;
import java.util.regex.Pattern;

public class DomainError extends RuntimeException {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9]*\\.[a-z0-9_]+$");

    private final String code;
    private final DomainErrorCategory category;

    public DomainError(String code, String message) {
        this(code, message, DomainErrorCategory.VALIDATION);
    }

    public DomainError(String code, String message, Throwable cause) {
        this(code, message, DomainErrorCategory.VALIDATION, cause);
    }

    public DomainError(String code, String message, DomainErrorCategory category) {
        this(code, message, category, null);
    }

    public DomainError(String code, String message, DomainErrorCategory category, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(category, "category must not be null");
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Invalid domain error code format '" + code + "'; expected <module>.<snake_case>");
        }
        this.code = code;
        this.category = category;
    }

    public static DomainError notFound(String code, String message) {
        return new DomainError(code, message, DomainErrorCategory.NOT_FOUND);
    }

    public static DomainError conflict(String code, String message) {
        return new DomainError(code, message, DomainErrorCategory.CONFLICT);
    }

    public static DomainError forbidden(String code, String message) {
        return new DomainError(code, message, DomainErrorCategory.FORBIDDEN);
    }

    public static DomainError internal(String code, String message) {
        return new DomainError(code, message, DomainErrorCategory.INTERNAL);
    }

    public String code() {
        return code;
    }

    public DomainErrorCategory category() {
        return category;
    }
}
