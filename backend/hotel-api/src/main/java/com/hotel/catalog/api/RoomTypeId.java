package com.hotel.catalog.api;

import com.hotel.sharedkernel.api.TypedId;
import java.util.UUID;

public final class RoomTypeId extends TypedId {

    public RoomTypeId(UUID value) {
        super(value);
    }

    public static RoomTypeId generate() {
        return new RoomTypeId(newUuidV7());
    }

    public static RoomTypeId of(UUID value) {
        return new RoomTypeId(value);
    }

    public static RoomTypeId fromString(String raw) {
        return new RoomTypeId(parseUuid(raw));
    }
}
