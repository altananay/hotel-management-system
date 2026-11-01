package com.hotel.adminaudit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotel.adminaudit.api.AdminAuditApi.AuditEvent;
import com.hotel.adminaudit.api.AuditReasonCode;
import com.hotel.adminaudit.api.AuditResult;
import com.hotel.adminaudit.internal.application.AdminAuditService;
import com.hotel.adminaudit.internal.persistence.AuditEntryEntity;
import com.hotel.adminaudit.internal.persistence.AuditEntryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock private AuditEntryRepository repository;

    private AdminAuditService service;

    @BeforeEach
    void setUp() {
        service =
                new AdminAuditService(
                        repository,
                        Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC));
        when(repository.save(any(AuditEntryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldMapLoginSuccessToAuditRow_whenRecordingEvent() {
        UUID actor = UUID.randomUUID();
        service.record(
                new AuditEvent(
                        null,
                        AuditReasonCode.LOGIN_SUCCESS,
                        AuditResult.SUCCESS,
                        actor,
                        "superadmin",
                        "127.0.0.1",
                        "JUnit",
                        "request-12345678"));

        ArgumentCaptor<AuditEntryEntity> captor =
                ArgumentCaptor.forClass(AuditEntryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("LOGIN");
        assertThat(captor.getValue().getReasonCode()).isEqualTo("LOGIN_SUCCESS");
        assertThat(captor.getValue().getResult()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getActorUserId()).isEqualTo(actor);
        assertThat(captor.getValue().getOccurredAt()).isEqualTo("2026-10-29T10:00:00Z");
    }

    @Test
    void shouldTruncateUserAgentTo512Characters_whenRecordingEvent() {
        service.record(
                new AuditEvent(
                        Instant.parse("2026-10-29T10:00:00Z"),
                        AuditReasonCode.LOGIN_INVALID_CREDENTIALS,
                        AuditResult.FAILURE,
                        null,
                        "unknown",
                        "127.0.0.1",
                        "x".repeat(600),
                        "request-12345678"));

        ArgumentCaptor<AuditEntryEntity> captor =
                ArgumentCaptor.forClass(AuditEntryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).hasSize(512);
        assertThat(captor.getValue().getResult()).isEqualTo("FAILURE");
    }
}
