package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.media.api.MediaStorageException;
import com.hotel.media.internal.persistence.MediaPublicCleanupTaskEntity;
import com.hotel.media.internal.persistence.MediaPublicCleanupTaskJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for CR-1 public-object cleanup compensation. */
@ExtendWith(MockitoExtension.class)
class MediaPublicCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @Mock private MediaPublicCleanupTaskJpaRepository repository;
    @Mock private MediaStoragePort storage;

    private MediaPublicCleanupService service;

    @BeforeEach
    void setUp() {
        service =
                new MediaPublicCleanupService(
                        repository,
                        storage,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        java.time.Duration.ofMinutes(5));
    }

    @Test
    void shouldEnqueueOnlyKeysThatAreNotAlreadyPresent() {
        when(repository.existsByPublicStorageKey("public/a.webp")).thenReturn(false);
        when(repository.existsByPublicStorageKey("public/b.webp")).thenReturn(true);

        service.enqueueAll(List.of("public/a.webp", "public/b.webp"));

        verify(repository).save(any());
    }

    @Test
    void shouldDeleteObjectAndTaskWhenCleanupSucceeds() {
        MediaPublicCleanupTaskEntity task = task();
        when(repository.findByPublicStorageKey("public/a.webp"))
                .thenReturn(Optional.of(task));

        service.processNow(List.of("public/a.webp"));

        verify(storage).delete("public/a.webp");
        verify(repository).delete(task);
    }

    @Test
    void shouldKeepTaskAndRecordAttemptWhenDeleteFails() {
        MediaPublicCleanupTaskEntity task = task();
        when(repository.findByPublicStorageKey("public/a.webp"))
                .thenReturn(Optional.of(task));
        org.mockito.Mockito.doThrow(
                        new MediaStorageException("down", new RuntimeException()))
                .when(storage)
                .delete("public/a.webp");

        service.processNow(List.of("public/a.webp"));

        assertThat(task.getAttempts()).isEqualTo((short) 1);
        assertThat(task.getLastAttemptAt()).isEqualTo(NOW);
        verify(repository).save(task);
        verify(repository, never()).delete(task);
    }

    @Test
    void shouldProcessPendingTasks() {
        MediaPublicCleanupTaskEntity task = task();
        when(repository.findTop50ByCreatedAtBeforeOrderByCreatedAtAsc(
                        NOW.minus(java.time.Duration.ofMinutes(5))))
                .thenReturn(List.of(task));

        service.processPending();

        verify(storage).delete("public/a.webp");
        verify(repository).delete(task);
    }

    private static MediaPublicCleanupTaskEntity task() {
        return new MediaPublicCleanupTaskEntity(
                UUID.fromString("0199f2c0-0000-7000-8000-000000000501"),
                "public/a.webp",
                NOW);
    }
}
