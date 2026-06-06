package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;

class AdminAiBatchRunLogQueryServiceTest {

    private AdminAiBatchRunLogQueryRepository repository;
    private AdminAiBatchRunLogQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminAiBatchRunLogQueryRepository.class);
        service = new AdminAiBatchRunLogQueryService(repository);
    }

    @Test
    void listBatchRunLogsReturnsPageAndConvertsCreatedAtToKstOffsetDateTime() {
        when(repository.findAll(any(AdminAiBatchRunLogQueryRepository.Filter.class), any(Pageable.class)))
                .thenReturn(new AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage(
                        List.of(new AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogRow(
                                4L,
                                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                                AiBatchRunStatus.FAILED,
                                0,
                                1,
                                0,
                                "ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND",
                                "active prompt not found",
                                OffsetDateTime.parse("2026-06-02T00:05:00+09:00"),
                                OffsetDateTime.parse("2026-06-02T00:05:01+09:00"),
                                LocalDateTime.parse("2026-06-02T00:05:02")
                        )),
                        12L
                ));

        AdminAiBatchRunLogListResponse response = service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L,
                "ADMIN",
                "OPERATOR",
                "AI_DAILY_QT_VERSE_EXPLANATION_SEED",
                "FAILED",
                "2026-06-01",
                "2026-06-02",
                1,
                5
        ));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).createdAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-02T00:05:02+09:00"));
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(12L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.sort()).isEqualTo("createdAt,desc,id,desc");

        ArgumentCaptor<AdminAiBatchRunLogQueryRepository.Filter> filterCaptor =
                ArgumentCaptor.forClass(AdminAiBatchRunLogQueryRepository.Filter.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(filterCaptor.capture(), pageableCaptor.capture());
        assertThat(filterCaptor.getValue().batchName()).isEqualTo(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED);
        assertThat(filterCaptor.getValue().status()).isEqualTo(AiBatchRunStatus.FAILED);
        assertThat(filterCaptor.getValue().fromCreatedAt()).isEqualTo(LocalDateTime.parse("2026-06-01T00:00:00"));
        assertThat(filterCaptor.getValue().toCreatedAtExclusive())
                .isEqualTo(LocalDateTime.parse("2026-06-03T00:00:00"));
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void reviewerAndSuperAdminCanListBatchRunLogs() {
        when(repository.findAll(any(AdminAiBatchRunLogQueryRepository.Filter.class), any(Pageable.class)))
                .thenReturn(new AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage(List.of(), 0L));

        service.listAdminAiBatchRunLogs(listQuery("REVIEWER"));
        service.listAdminAiBatchRunLogs(listQuery("SUPER_ADMIN"));
    }

    @Test
    void contentCreatorOrNonAdminCannotListBatchRunLogs() {
        assertForbidden(() -> service.listAdminAiBatchRunLogs(listQuery("CONTENT_CREATOR")));
        assertForbidden(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "USER", "OPERATOR", null, null, null, null, 0, 20
        )));
    }

    @Test
    void invalidQueryThrowsInvalidInput() {
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(null));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                0L, "ADMIN", "OPERATOR", null, null, null, null, 0, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", "UNKNOWN", null, null, null, 0, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", null, "UNKNOWN", null, null, 0, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", null, null, "2026-06-03", "2026-06-02", 0, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", null, null, "bad-date", null, 0, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", null, null, null, null, -1, 20
        )));
        assertInvalidInput(() -> service.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                7L, "ADMIN", "OPERATOR", null, null, null, null, 0, 101
        )));
    }

    private static ListAdminAiBatchRunLogsQuery listQuery(String adminRole) {
        return new ListAdminAiBatchRunLogsQuery(7L, "ADMIN", adminRole, null, null, null, null, 0, 20);
    }

    private static void assertForbidden(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private static void assertInvalidInput(Runnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
