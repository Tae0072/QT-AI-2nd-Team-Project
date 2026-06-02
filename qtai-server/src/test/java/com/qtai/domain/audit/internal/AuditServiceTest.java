package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogFilter;
import com.qtai.domain.audit.api.dto.AuditLogResponse;
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
    void serviceImplementsListAuditUseCase() {
        assertThat(service).isInstanceOf(ListAuditUseCase.class);
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

    @Test
    void listForwardsFilterAndMapsToResponse() {
        AuditLog log = AuditLog.create(
                null, "ADMIN", 7L, "ADMIN:7", "CHECKLIST_CREATE",
                "AI_VALIDATION_CHECKLIST_VERSION", 4L, null, "{}",
                OffsetDateTime.parse("2026-05-27T10:00:00+09:00"));
        setId(log, 100L);
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.search(eq("ADMIN"), eq(7L), eq("CHECKLIST_CREATE"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        Page<AuditLogResponse> page = service.list(
                new AuditLogFilter("ADMIN", 7L, "CHECKLIST_CREATE", null, null), pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        AuditLogResponse r = page.getContent().get(0);
        assertThat(r.id()).isEqualTo(100L);
        assertThat(r.actorType()).isEqualTo("ADMIN");
        assertThat(r.actorId()).isEqualTo(7L);
        assertThat(r.actionType()).isEqualTo("CHECKLIST_CREATE");
        assertThat(r.targetType()).isEqualTo("AI_VALIDATION_CHECKLIST_VERSION");
        assertThat(r.targetId()).isEqualTo(4L);
    }

    @Test
    void listWithNullFilterQueriesAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AuditLogResponse> page = service.list(null, pageable);

        assertThat(page.getContent()).isEmpty();
    }

    private static void setId(AuditLog log, Long id) {
        try {
            Field f = AuditLog.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(log, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
