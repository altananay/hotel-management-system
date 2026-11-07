package com.hotel.media.internal.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MediaVariantJpaRepository extends JpaRepository<MediaVariantEntity, UUID> {

    List<MediaVariantEntity> findByMediaAssetIdIn(Collection<UUID> mediaAssetIds);

    List<MediaVariantEntity> findByMediaAssetId(UUID mediaAssetId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MediaVariantEntity v WHERE v.mediaAssetId = :mediaAssetId")
    void deleteByMediaAssetId(@Param("mediaAssetId") UUID mediaAssetId);
}
