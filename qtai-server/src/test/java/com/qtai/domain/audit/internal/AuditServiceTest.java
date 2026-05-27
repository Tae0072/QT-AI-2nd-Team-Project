package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

class AuditServiceTest {

    private AuditRepository repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AuditRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-27T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new AuditService(repository, clock);
    }

    @Test
    void serviceImplementsWriteAuditLogUseCase() {
        assertThat(service).isInstanceOf(WriteAuditLogUseCase.class);
    }

    @Test
    void writeStoresAppendOnlyAuditLog() {
        service.write(new AuditLogWriteRequest(
                null,
                "ADMIN",
                7L,
                "ADMIN:7",
                "CHECKLIST_CREATE",
                "AI_VALIDATION_CHECKLIST_VERSION",
                4L,
                null,
                "{\"id\":4,\"status\":\"DRAFT\"}"
        ));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getAdminUserId()).isNull();
        assertThat(auditLog.getActorType()).isEqualTo("ADMIN");
        assertThat(auditLog.getActorId()).isEqualTo(7L);
        assertThat(auditLog.getActionType()).isEqualTo("CHECKLIST_CREATE");
        assertThat(auditLog.getAfterJson()).contains("\"status\":\"DRAFT\"");
        assertThat(auditLog.getAfterJson())
                .doesNotContain(
                        "raw " + "response",
                        "provider " + "raw",
                        "private " + "key",
                        "sample " + "token"
                );
        assertThat(auditLog.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-05-27T10:00:00+09:00"));
    }

    @Test
    void writeRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> service.write(new AuditLogWriteRequest(
                null,
                "",
                7L,
                "ADMIN:7",
                "CHECKLIST_CREATE",
                "AI_VALIDATION_CHECKLIST_VERSION",
                4L,
                null,
                "{}"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actorType must not be blank");

        verify(repository, org.mockito.Mockito.never()).save(any(AuditLog.class));
    }
}
