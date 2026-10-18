package com.hotel.sharedkernel.api;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public abstract class TypedId implements Serializable, Comparable<TypedId> {

    public static final String INVALID_ID_CODE = "sharedkernel.invalid_id";

    private static final Pattern UUID_CANONICAL_PATTERN =
            Pattern.compile(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final TimeBasedEpochGenerator UUID_V7_GENERATOR =
            Generators.timeBasedEpochGenerator();

    private final UUID value;

    protected TypedId(UUID value) {
        if (value == null) {
            throw new DomainError(INVALID_ID_CODE, "Identifier UUID must not be null");
        }
        this.value = value;
    }

    protected static UUID newUuidV7() {
        return UUID_V7_GENERATOR.generate();
    }

    protected static UUID parseUuid(String raw) {
        if (raw == null || !UUID_CANONICAL_PATTERN.matcher(raw).matches()) {
            throw new DomainError(INVALID_ID_CODE, "Invalid UUID identifier format");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new DomainError(INVALID_ID_CODE, "Invalid UUID identifier format", ex);
        }
    }

    public final UUID value() {
        return value;
    }

    @Override
    public final int compareTo(TypedId other) {
        Objects.requireNonNull(other, "other must not be null");
        if (this.getClass() != other.getClass()) {
            throw new ClassCastException(
                    "Cannot compare different TypedId types: "
                            + this.getClass().getName()
                            + " and "
                            + other.getClass().getName());
        }
        return this.value.toString().compareTo(other.value.toString());
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        TypedId other = (TypedId) obj;
        return this.value.equals(other.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), value);
    }

    @Override
    public final String toString() {
        return value.toString();
    }
}
