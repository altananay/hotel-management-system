package com.hotel.media.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.media.internal.persistence.MediaPublicCleanupTaskEntity;
import com.hotel.media.internal.persistence.MediaPublicCleanupTaskJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists best-effort deletion of public S3 objects and retries failures. This keeps the
 * database committed state authoritative while compensating for the non-transactional S3 side.
 */
@Service
public class MediaPublicCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MediaPublicCleanupService.class);

    private final MediaPublicCleanupTaskJpaRepository repository;
    private final MediaStoragePort storage;
    private final Clock clock;
    private final Duration grace;

    public MediaPublicCleanupService(
            MediaPublicCleanupTaskJpaRepository repository,
            MediaStoragePort storage,
            Clock clock,
            @Value("${hotel.media.cleanup.grace:5m}") Duration grace) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
        this.grace = grace;
    }

    /** Disarms cleanup intents after a successful publish, in the publish transaction. */
    @Transactional
    public void removeAll(Collection<String> publicKeys) {
        repository.deleteByPublicStorageKeyIn(publicKeys);
    }

    @Transactional
    public void enqueueAll(Collection<String> publicKeys) {
        for (String key : publicKeys) {
            if (key != null && !repository.existsByPublicStorageKey(key)) {
                repository.save(newTask(key));
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueAllInNewTransaction(Collection<String> publicKeys) {
        for (String key : publicKeys) {
            if (key != null && !repository.existsByPublicStorageKey(key)) {
                try {
                    repository.save(newTask(key));
                } catch (DataIntegrityViolationException ignored) {
                    // Another request already queued the same immutable key.
                }
            }
        }
    }

    /** Tries to delete the given keys immediately; failures remain queued for the scheduler. */
    public void processNow(Collection<String> publicKeys) {
        for (String key : publicKeys) {
            repository
                    .findByPublicStorageKey(key)
                    .ifPresent(this::processTask);
        }
    }

    @Scheduled(
            initialDelayString = "${hotel.media.cleanup.initial-delay:30s}",
            fixedDelayString = "${hotel.media.cleanup.interval:60s}")
    public void processPending() {
        // Grace period prevents a scheduled cleanup from racing a publish that has copied
        // objects but has not committed its DB transaction yet.
        Instant cutoff = clock.instant().minus(grace);
        for (MediaPublicCleanupTaskEntity task :
                repository.findTop50ByCreatedAtBeforeOrderByCreatedAtAsc(cutoff)) {
            processTask(task);
        }
    }

    private void processTask(MediaPublicCleanupTaskEntity task) {
        try {
            storage.delete(task.getPublicStorageKey());
            repository.delete(task);
        } catch (RuntimeException ex) {
            task.recordAttempt(clock.instant());
            repository.save(task);
            log.warn(
                    "Could not delete public media object {}; cleanup will retry.",
                    task.getPublicStorageKey(),
                    ex);
        }
    }

    private MediaPublicCleanupTaskEntity newTask(String key) {
        Instant now = clock.instant();
        return new MediaPublicCleanupTaskEntity(
                Generators.timeBasedEpochGenerator().generate(), key, now);
    }
}
