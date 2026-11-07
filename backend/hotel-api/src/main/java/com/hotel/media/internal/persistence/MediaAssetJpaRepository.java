package com.hotel.media.internal.persistence;

import com.hotel.media.internal.domain.MediaStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MediaAssetJpaRepository extends JpaRepository<MediaAssetEntity, UUID> {

    @Query(
            "SELECT a FROM MediaAssetEntity a "
                    + "WHERE (:roomTypeId IS NULL OR a.roomTypeId = :roomTypeId) "
                    + "AND (:status IS NULL OR a.status = :status) "
                    + "AND (:includeArchived = TRUE OR a.archivedAt IS NULL) "
                    + "ORDER BY a.updatedAt DESC, a.id DESC")
    Page<MediaAssetEntity> findAdminPage(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("status") MediaStatus status,
            @Param("includeArchived") boolean includeArchived,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaAssetEntity> findLockedById(UUID id);

    /**
     * Returns an empty list when the asset does not exist and a list containing {@code null} when
     * the asset exists without a room type. This keeps "missing asset" distinguishable from
     * "asset without room type" (QA-01).
     */
    @Query("SELECT a.roomTypeId FROM MediaAssetEntity a WHERE a.id = :id")
    List<UUID> findRoomTypeIdsById(@Param("id") UUID id);

    @Query(
            "SELECT a FROM MediaAssetEntity a "
                    + "WHERE a.roomTypeId = :roomTypeId AND a.archivedAt IS NULL "
                    + "ORDER BY a.displayOrder ASC, a.id ASC")
    List<MediaAssetEntity> findNonArchivedByRoomTypeOrdered(
            @Param("roomTypeId") UUID roomTypeId);

    /** Serializes cover/order/assignment mutations for one room in a stable id order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT a FROM MediaAssetEntity a "
                    + "WHERE a.roomTypeId = :roomTypeId AND a.archivedAt IS NULL "
                    + "ORDER BY a.id ASC")
    List<MediaAssetEntity> lockRoomMedia(@Param("roomTypeId") UUID roomTypeId);

    @Query(
            "SELECT COALESCE(MAX(a.displayOrder), -1) FROM MediaAssetEntity a "
                    + "WHERE a.roomTypeId = :roomTypeId AND a.archivedAt IS NULL")
    int maxDisplayOrder(@Param("roomTypeId") UUID roomTypeId);

    @Query(
            "SELECT a FROM MediaAssetEntity a "
                    + "WHERE a.status = :status AND a.archivedAt IS NULL "
                    + "AND (a.processingStartedAt IS NULL "
                    + "OR a.processingStartedAt < :staleBefore)")
    List<MediaAssetEntity> findStaleProcessing(
            @Param("status") MediaStatus status, @Param("staleBefore") Instant staleBefore);

    @Query(
            "SELECT a FROM MediaAssetEntity a "
                    + "WHERE a.status = :status AND a.publishedAt IS NOT NULL "
                    + "AND a.archivedAt IS NULL AND a.roomTypeId IN :roomTypeIds "
                    + "ORDER BY a.roomTypeId, a.displayOrder ASC, a.id ASC")
    List<MediaAssetEntity> findPublishedByRoomTypeIds(
            @Param("status") MediaStatus status,
            @Param("roomTypeIds") Collection<UUID> roomTypeIds);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.processingStartedAt = :now, "
                    + "a.processingLeaseId = :leaseId, "
                    + "a.processingAttempts = a.processingAttempts + 1, a.updatedAt = :now "
                    + "WHERE a.id = :id AND a.status = :status AND a.archivedAt IS NULL "
                    + "AND a.processingAttempts < :maxAttempts "
                    + "AND (a.processingStartedAt IS NULL OR a.processingStartedAt < :staleBefore)")
    int claimForProcessing(
            @Param("id") UUID id,
            @Param("status") MediaStatus status,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("leaseId") UUID leaseId,
            @Param("maxAttempts") int maxAttempts);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.processingStartedAt = NULL, "
                    + "a.processingLeaseId = NULL, a.updatedAt = :now "
                    + "WHERE a.id = :id AND a.processingLeaseId = :leaseId")
    int releaseProcessingClaim(
            @Param("id") UUID id, @Param("leaseId") UUID leaseId, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.status = com.hotel.media.internal.domain.MediaStatus.READY, "
                    + "a.failureCode = NULL, a.processingStartedAt = NULL, "
                    + "a.processingLeaseId = NULL, a.updatedAt = :now "
                    + "WHERE a.id = :id AND a.status = com.hotel.media.internal.domain.MediaStatus.PROCESSING "
                    + "AND a.processingLeaseId = :leaseId AND a.archivedAt IS NULL")
    int markReadyIfClaimed(
            @Param("id") UUID id, @Param("leaseId") UUID leaseId, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.status = com.hotel.media.internal.domain.MediaStatus.FAILED, "
                    + "a.failureCode = :failureCode, a.processingStartedAt = NULL, "
                    + "a.processingLeaseId = NULL, a.updatedAt = :now "
                    + "WHERE a.id = :id AND a.status = com.hotel.media.internal.domain.MediaStatus.PROCESSING "
                    + "AND (a.processingLeaseId = :leaseId OR a.processingLeaseId IS NULL) "
                    + "AND a.archivedAt IS NULL")
    int markFailedIfClaimed(
            @Param("id") UUID id,
            @Param("leaseId") UUID leaseId,
            @Param("failureCode") String failureCode,
            @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.status = com.hotel.media.internal.domain.MediaStatus.FAILED, "
                    + "a.failureCode = :failureCode, a.processingStartedAt = NULL, "
                    + "a.processingLeaseId = NULL, a.updatedAt = :now "
                    + "WHERE a.id = :id AND a.status = com.hotel.media.internal.domain.MediaStatus.PROCESSING "
                    + "AND a.archivedAt IS NULL AND a.processingAttempts >= :maxAttempts "
                    + "AND (a.processingStartedAt IS NULL OR a.processingStartedAt < :staleBefore)")
    int markFailedIfExhausted(
            @Param("id") UUID id,
            @Param("failureCode") String failureCode,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("maxAttempts") int maxAttempts);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.cover = FALSE, a.updatedAt = :now "
                    + "WHERE a.roomTypeId = :roomTypeId AND a.publishedAt IS NOT NULL "
                    + "AND a.archivedAt IS NULL")
    void clearCoverForRoom(@Param("roomTypeId") UUID roomTypeId, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.displayOrder = a.displayOrder + 100000, "
                    + "a.updatedAt = :now WHERE a.roomTypeId = :roomTypeId "
                    + "AND a.archivedAt IS NULL")
    void shiftDisplayOrders(@Param("roomTypeId") UUID roomTypeId, @Param("now") Instant now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE MediaAssetEntity a SET a.displayOrder = :displayOrder, a.updatedAt = :now "
                    + "WHERE a.id = :id")
    void setDisplayOrder(
            @Param("id") UUID id,
            @Param("displayOrder") int displayOrder,
            @Param("now") Instant now);
}
