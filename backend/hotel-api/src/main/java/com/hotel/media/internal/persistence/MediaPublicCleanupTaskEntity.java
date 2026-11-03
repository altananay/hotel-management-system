package com.hotel.media.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "media", name = "media_public_cleanup_tasks")
public class MediaPublicCleanupTaskEntity {

    @Id private UUID id;

    @Column(name = "public_storage_key", nullable = false, unique = true, length = 512)
    private String publicStorageKey;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    protected MediaPublicCleanupTaskEntity() {}

    public MediaPublicCleanupTaskEntity(UUID id, String publicStorageKey, Instant createdAt) {
        this.id = id;
        this.publicStorageKey = publicStorageKey;
        this.attempts = 0;
        this.createdAt = createdAt;
    }

    public void recordAttempt(Instant now) {
        this.attempts++;
        this.lastAttemptAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getPublicStorageKey() {
        return publicStorageKey;
    }

    public short getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }
}
