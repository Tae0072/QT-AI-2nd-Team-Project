package com.qtai.domain.audit.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;

class AuditQueryServiceTest {

    private AuditQueryRepository queryRepository;
    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        queryRepository = org.mockito.Mockito.mock(AuditQueryRepository.class);
        service = new AuditQueryService(queryRepository);
    }

    @Test
    void serviceImplementsListAuditUseCase() {
        assertThat(service).isInstanceOf(ListAuditUseCase.class);
    }

    @Test
    void listAuditLogsValidatesAndMapsFilters() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-02T10:30:00+09:00");
        when(queryRepository.findAll(any(AuditQueryRepository.Filter.class), any(PageRequest.class)))
                .thenReturn(new AuditQueryRepository.AuditLogPage(
                        List.of(new AuditQueryRepository.AuditLogRow(
                                1L,
                                null,
                                "ADMIN",
                                7L,
                                "ADMIN:7",
                                "AI_ASSET_APPROVE",
                                "AI_GENERATED_ASSET",
                                500L,
                                "{\"status\":\"VALIDATING\"}",
                                "{\"status\":\"APPROVED\"}",
                                createdAt
                        )),
                        1L
                ));

        AuditLogListResponse response = service.listAuditLogs(new ListAuditQuery(
                9L,
                "ADMIN",
                "REVIEWER",
                "ADMIN",
                7L,
                "AI_ASSET_APPROVE",
                "AI_GENERATED_ASSET",
                500L,
                "2026-06-01",
                "2026-06-02",
                0,
                20
        ));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().actionType()).isEqualTo("AI_ASSET_APPROVE");
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.sort()).isEqualTo("createdAt,desc,id,desc");

        ArgumentCaptor<AuditQueryRepository.Filter> filterCaptor =
                ArgumentCaptor.forClass(AuditQueryRepository.Filter.class);
        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(queryRepository).findAll(filterCaptor.capture(), pageCaptor.capture());
        AuditQueryRepository.Filter filter = filterCaptor.getValue();
        assertThat(filter.actorType()).isEqualTo("ADMIN");
        assertThat(filter.actorId()).isEqualTo(7L);
        assertThat(filter.actionTypes()).containsExactly("AI_ASSET_APPROVE");
        assertThat(filter.targetType()).isEqualTo("AI_GENERATED_ASSET");
        assertThat(filter.targetId()).isEqualTo(500L);
        assertThat(filter.from()).isEqualTo(OffsetDateTime.parse("2026-06-01T00:00:00+09:00"));
        assertThat(filter.to()).isEqualTo(OffsetDateTime.parse("2026-06-03T00:00:00+09:00"));
        assertThat(pageCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void listAuditLogsDefaultsToAiAuditScope() {
        when(queryRepository.findAll(any(AuditQueryRepository.Filter.class), any(PageRequest.class)))
                .thenReturn(new AuditQueryRepository.AuditLogPage(List.of(), 0L));

        service.listAuditLogs(new ListAuditQuery(
                9L,
                "ADMIN",
                "OPERATOR",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        ));

        ArgumentCaptor<AuditQueryRepository.Filter> filterCaptor =
                ArgumentCaptor.forClass(AuditQueryRepository.Filter.class);
        verify(queryRepository).findAll(filterCaptor.capture(), any(PageRequest.class));
        assertThat(filterCaptor.getValue().actionTypes())
                .containsExactly(
                        "AI_ASSET_APPROVE",
                        "AI_ASSET_REJECT",
                        "AI_ASSET_HIDE",
                        "AI_REGENERATE_REQUEST"
                );
        assertThat(filterCaptor.getValue().targetType()).isEqualTo("AI_GENERATED_ASSET");
    }

    @Test
    void reviewerAndSuperAdminCanListAuditLogs() {
        when(queryRepository.findAll(any(AuditQueryRepository.Filter.class), any(PageRequest.class)))
                .thenReturn(new AuditQueryRepository.AuditLogPage(List.of(), 0L));

        service.listAuditLogs(new ListAuditQuery(
                9L, "ADMIN", "REVIEWER", null, null, null, null, null, null, null, 0, 20
        ));
        service.listAuditLogs(new ListAuditQuery(
                9L, "ADMIN", "SUPER_ADMIN", null, null, null, null, null, null, null, 0, 20
        ));
    }

    @Test
    void listAuditLogsRejectsUnauthorizedAdminRoles() {
        assertForbidden(new ListAuditQuery(1L, "USER", "OPERATOR", null, null, null, null, null, null, null, 0, 20));
        assertForbidden(new ListAuditQuery(1L, "ADMIN", "CONTENT_CREATOR", null, null, null, null, null, null, null, 0, 20));

        verify(queryRepository, never()).findAll(any(), any());
    }

    @Test
    void listAuditLogsRejectsInvalidInputs() {
        assertInvalid(new ListAuditQuery(0L, "ADMIN", "OPERATOR", null, null, null, null, null, null, null, 0, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, "LOGIN", null, null, null, null, 0, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, "MEMBER", null, null, null, 0, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, null, null, "2026-06-03", "2026-06-02", 0, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, null, null, "bad-date", null, 0, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, null, null, null, null, -1, 20));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, null, null, null, null, 0, 0));
        assertInvalid(new ListAuditQuery(1L, "ADMIN", "OPERATOR", null, null, null, null, null, null, null, 0, 101));

        verify(queryRepository, never()).findAll(any(), any());
    }

    private void assertInvalid(ListAuditQuery query) {
        assertThatThrownBy(() -> service.listAuditLogs(query))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private void assertForbidden(ListAuditQuery query) {
        assertThatThrownBy(() -> service.listAuditLogs(query))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
