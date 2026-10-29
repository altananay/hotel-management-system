package com.hotel.adminidentity.internal.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLoginAttemptRepository
        extends JpaRepository<AdminLoginAttemptEntity, UUID> {

    @Query(
            "select a from AdminLoginAttemptEntity a "
                    + "where a.identifierNorm = :key and a.ipAddress = :ip "
                    + "and a.lockedUntil is not null and a.lockedUntil > :now "
                    + "order by a.lockedUntil desc")
    List<AdminLoginAttemptEntity> findActiveLocks(
            @Param("key") String key,
            @Param("ip") String ip,
            @Param("now") Instant now);

    @Query(
            "select count(a) from AdminLoginAttemptEntity a "
                    + "where a.identifierNorm = :key and a.ipAddress = :ip "
                    + "and a.attemptedAt >= :since")
    long countRecentFailures(
            @Param("key") String key,
            @Param("ip") String ip,
            @Param("since") Instant since);

    void deleteByIdentifierNormAndIpAddress(String identifierNorm, String ipAddress);

    Optional<AdminLoginAttemptEntity> findTopByIdentifierNormAndIpAddressOrderByAttemptedAtDesc(
            String identifierNorm, String ipAddress);
}
