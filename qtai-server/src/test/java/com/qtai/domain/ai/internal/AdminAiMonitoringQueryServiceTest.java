package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.dto.GetAdminAiMonitoringQuery;

class AdminAiMonitoringQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-02T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private AdminAiMonitoringQueryRepository repository;
    private AdminAiMonitoringQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminAiMonitoringQueryRepository.class);
        service = new AdminAiMonitoringQueryService(repository, CLOCK);
    }

    @Test
    void getMonitoringUsesDefaultKstTodayAndReturnsSummary() {
        when(repository.summarize(any(AdminAiMonitoringQueryRepository.Filter.class)))
                .thenReturn(sampleSummary());

        AdminAiMonitoringResponse response = service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L,
                "ADMIN",
                "OPERATOR",
                null,
                null
        ));

        assertThat(response.period().from()).isEqualTo(LocalDate.parse("2026-06-02"));
        assertThat(response.period().to()).isEqualTo(LocalDate.parse("2026-06-02"));
        assertThat(response.period().timezone()).isEqualTo("Asia/Seoul");
        assertThat(response.generationJobs().queued()).isEqualTo(3);
        assertThat(response.validation().failureReasons())
                .extracting(AdminAiMonitoringResponse.FailureReason::resultCode)
                .containsExactly("SOURCE_MISSING");
        assertThat(response.batchRuns().latestFailures().get(0).createdAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-02T00:05:10+09:00"));
        assertThat(response.qa().requested()).isZero();
        assertThat(response.qa().blockedReasons()).isEmpty();
        assertThat(response.checklists().get(0).passRate()).isEqualTo(0.5);

        ArgumentCaptor<AdminAiMonitoringQueryRepository.Filter> filterCaptor =
                ArgumentCaptor.forClass(AdminAiMonitoringQueryRepository.Filter.class);
        verify(repository).summarize(filterCaptor.capture());
        AdminAiMonitoringQueryRepository.Filter filter = filterCaptor.getValue();
        assertThat(filter.fromAt()).isEqualTo(OffsetDateTime.parse("2026-06-02T00:00:00+09:00"));
        assertThat(filter.toAtExclusive()).isEqualTo(OffsetDateTime.parse("2026-06-03T00:00:00+09:00"));
        assertThat(filter.fromCreatedAt()).isEqualTo(LocalDateTime.parse("2026-06-02T00:00:00"));
        assertThat(filter.toCreatedAtExclusive()).isEqualTo(LocalDateTime.parse("2026-06-03T00:00:00"));
    }

    @Test
    void getMonitoringAcceptsExplicitPeriod() {
        when(repository.summarize(any(AdminAiMonitoringQueryRepository.Filter.class)))
                .thenReturn(emptySummary());

        AdminAiMonitoringResponse response = service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L,
                "ADMIN",
                "SUPER_ADMIN",
                "2026-06-01",
                "2026-06-02"
        ));

        assertThat(response.period().from()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(response.period().to()).isEqualTo(LocalDate.parse("2026-06-02"));

        ArgumentCaptor<AdminAiMonitoringQueryRepository.Filter> filterCaptor =
                ArgumentCaptor.forClass(AdminAiMonitoringQueryRepository.Filter.class);
        verify(repository).summarize(filterCaptor.capture());
        assertThat(filterCaptor.getValue().fromAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-01T00:00:00+09:00"));
        assertThat(filterCaptor.getValue().toAtExclusive())
                .isEqualTo(OffsetDateTime.parse("2026-06-03T00:00:00+09:00"));
    }

    @Test
    void reviewerCanGetMonitoringButContentCreatorOrNonAdminCannot() {
        when(repository.summarize(any(AdminAiMonitoringQueryRepository.Filter.class)))
                .thenReturn(emptySummary());

        service.getAdminAiMonitoring(query("REVIEWER"));
        service.getAdminAiMonitoring(query("SUPER_ADMIN"));

        assertForbidden(() -> service.getAdminAiMonitoring(query("CONTENT_CREATOR")));
        assertForbidden(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L, "USER", "OPERATOR", null, null
        )));
    }

    @Test
    void invalidQueryThrowsInvalidInput() {
        assertInvalidInput(() -> service.getAdminAiMonitoring(null));
        assertInvalidInput(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                0L, "ADMIN", "OPERATOR", null, null
        )));
        assertInvalidInput(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L, " ", "OPERATOR", null, null
        )));
        assertInvalidInput(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L, "ADMIN", " ", null, null
        )));
        assertInvalidInput(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L, "ADMIN", "OPERATOR", "bad-date", null
        )));
        assertInvalidInput(() -> service.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                7L, "ADMIN", "OPERATOR", "2026-06-03", "2026-06-02"
        )));
    }

    private static GetAdminAiMonitoringQuery query(String adminRole) {
        return new GetAdminAiMonitoringQuery(7L, "ADMIN", adminRole, null, null);
    }

    private static AdminAiMonitoringQueryRepository.Summary sampleSummary() {
        return new AdminAiMonitoringQueryRepository.Summary(
                new AdminAiMonitoringQueryRepository.GenerationJobCounts(3, 1, 120, 4),
                new AdminAiMonitoringQueryRepository.ValidationCounts(8, 110, 10, 2),
                List.of(new AdminAiMonitoringQueryRepository.FailureReasonRow("SOURCE_MISSING", 4)),
                new AdminAiMonitoringQueryRepository.BatchRunCounts(5, 1, 2),
                List.of(new AdminAiMonitoringQueryRepository.BatchFailureRow(
                        9L,
                        AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                        AiBatchRunStatus.FAILED,
                        "IllegalStateException",
                        "failed",
                        LocalDateTime.parse("2026-06-02T00:05:10")
                )),
                List.of(new AdminAiMonitoringQueryRepository.ChecklistRow(
                        AiValidationChecklistType.EXPLANATION,
                        "2026.06.1",
                        1,
                        2
                ))
        );
    }

    private static AdminAiMonitoringQueryRepository.Summary emptySummary() {
        return new AdminAiMonitoringQueryRepository.Summary(
                new AdminAiMonitoringQueryRepository.GenerationJobCounts(0, 0, 0, 0),
                new AdminAiMonitoringQueryRepository.ValidationCounts(0, 0, 0, 0),
                List.of(),
                new AdminAiMonitoringQueryRepository.BatchRunCounts(0, 0, 0),
                List.of(),
                List.of()
        );
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
