package com.hotel.catalog.api;

import com.hotel.sharedkernel.api.TypedId;
import java.util.UUID;

public final class AmenityId extends TypedId {

    public AmenityId(UUID value) {
        super(value);
    }

    public static AmenityId generate() {
        return new AmenityId(newUuidV7());
    }

    public static AmenityId of(UUID value) {
        return new AmenityId(value);
    }

    public static AmenityId fromString(String raw) {
        return new AmenityId(parseUuid(raw));
    }
}
