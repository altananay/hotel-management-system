package com.hotel.media.internal.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaPublicCleanupTaskJpaRepository
        extends JpaRepository<MediaPublicCleanupTaskEntity, UUID> {

    Optional<MediaPublicCleanupTaskEntity> findByPublicStorageKey(String publicStorageKey);

    boolean existsByPublicStorageKey(String publicStorageKey);

    List<MediaPublicCleanupTaskEntity> findTop50ByOrderByCreatedAtAsc();

    List<MediaPublicCleanupTaskEntity>
            findTop50ByDeadLetteredAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                    Instant cutoff);

    void deleteByPublicStorageKeyIn(Collection<String> publicStorageKeys);
}
