package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.qtai.domain.audit.api.dto.AdminDashboardAuditLog;

@ExtendWith(MockitoExtension.class)
class AdminDashboardAuditLogServiceTest {

    @Mock
    AuditQueryRepository repository;

    @Test
    @DisplayName("최근 감사 로그를 dashboard sanitized DTO로 반환한다")
    void returns_sanitized_recent_audit_logs() {
        when(repository.findRecent(any(Pageable.class)))
                .thenReturn(List.of(new AuditQueryRepository.DashboardAuditLogRow(
                        10L,
                        1L,
                        "ADMIN",
                        "AI_ASSET_APPROVE",
                        "AI_GENERATED_ASSET",
                        500L,
                        OffsetDateTime.parse("2026-06-10T10:00:00+09:00")
                )));
        AdminDashboardAuditLogService service = new AdminDashboardAuditLogService(repository);

        List<AdminDashboardAuditLog> logs = service.listRecentAuditLogs(5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findRecent(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("createdAt"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(logs).singleElement().satisfies(log -> {
            assertThat(log.id()).isEqualTo(10L);
            assertThat(log.adminUserId()).isEqualTo(1L);
            assertThat(log.actorType()).isEqualTo("ADMIN");
            assertThat(log.actionType()).isEqualTo("AI_ASSET_APPROVE");
            assertThat(log.targetType()).isEqualTo("AI_GENERATED_ASSET");
            assertThat(log.targetId()).isEqualTo(500L);
            assertThat(log.createdAt()).isEqualTo(OffsetDateTime.parse("2026-06-10T10:00:00+09:00"));
        });
    }
}
