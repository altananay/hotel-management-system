package com.hotel.catalog.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only catalog port for modules that need to validate or display room types. Consumers must
 * use this port instead of importing catalog internals or querying catalog tables.
 */
public interface RoomTypeLookup {

    Optional<ActiveRoomType> findActive(RoomTypeId id);

    List<ActiveRoomType> findActiveByIds(Collection<RoomTypeId> ids);

    List<ActiveRoomType> findAllActive();

    /**
     * Returns room types in any status. Used by admin read surfaces that must keep showing
     * historical prices for room types that have since been deactivated.
     */
    List<RoomTypeSnapshot> findByIds(Collection<RoomTypeId> ids);
}
