package com.hotel.catalog.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only catalog port for modules that need to validate or display active room types. The media
 * module must use this port instead of importing catalog internals or querying catalog tables.
 */
public interface RoomTypeLookup {

    Optional<ActiveRoomType> findActive(RoomTypeId id);

    List<ActiveRoomType> findActiveByIds(Collection<RoomTypeId> ids);

    List<ActiveRoomType> findAllActive();
}
