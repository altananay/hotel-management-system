package com.hotel.adminaudit.internal.application;

import com.fasterxml.uuid.Generators;
import com.hotel.adminaudit.api.AdminAuditApi;
import com.hotel.adminaudit.internal.persistence.AuditEntryEntity;
import com.hotel.adminaudit.internal.persistence.AuditEntryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService implements AdminAuditApi {

    private final AuditEntryRepository repository;
    private final Clock clock;

    public AdminAuditService(AuditEntryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        Instant occurredAt =
                event.occurredAt() == null
                        ? clock.instant().truncatedTo(ChronoUnit.MILLIS)
                        : event.occurredAt();
        AuditEntryEntity entity =
                new AuditEntryEntity(
                        Generators.timeBasedEpochGenerator().generate(),
                        occurredAt,
                        event.reasonCode().eventType().name(),
                        event.actorUserId(),
                        truncate(event.usernameAttempted(), 254),
                        event.result().name(),
                        event.reasonCode().name(),
                        event.ipAddress(),
                        truncate(event.userAgent(), 512),
                        event.requestId());
        repository.save(entity);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
