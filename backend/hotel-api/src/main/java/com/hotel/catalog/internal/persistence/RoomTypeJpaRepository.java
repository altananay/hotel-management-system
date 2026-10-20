package com.hotel.catalog.internal.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeEntity, UUID> {

    @Query(
            value =
                    "SELECT * FROM catalog.room_types "
                            + "WHERE active = true "
                            + "ORDER BY display_order ASC, id ASC "
                            + "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<RoomTypeEntity> findActivePageOrdered(
            @Param("offset") long offset, @Param("limit") int limit);

    @Query(
            value = "SELECT COUNT(*) FROM catalog.room_types WHERE active = true",
            nativeQuery = true)
    long countActive();

    @Query(
            "SELECT DISTINCT rt FROM RoomTypeEntity rt "
                    + "LEFT JOIN FETCH rt.amenities "
                    + "WHERE rt.id IN :ids")
    List<RoomTypeEntity> findAllWithAmenitiesByIdIn(@Param("ids") Collection<UUID> ids);
}
