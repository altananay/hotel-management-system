package com.hotel.adminidentity.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "admin_platform", name = "admin_login_attempts")
public class AdminLoginAttemptEntity {

    @Id private UUID id;

    @Column(name = "identifier_norm", nullable = false, length = 254)
    private String identifierNorm;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected AdminLoginAttemptEntity() {}

    public AdminLoginAttemptEntity(
            UUID id,
            String identifierNorm,
            UUID userId,
            String ipAddress,
            Instant attemptedAt,
            Instant lockedUntil) {
        this.id = id;
        this.identifierNorm = identifierNorm;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.attemptedAt = attemptedAt;
        this.lockedUntil = lockedUntil;
    }

    public UUID getId() {
        return id;
    }

    public String getIdentifierNorm() {
        return identifierNorm;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
