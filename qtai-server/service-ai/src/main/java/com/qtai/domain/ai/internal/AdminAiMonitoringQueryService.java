package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;

@Service
class AdminAiMonitoringQueryService implements GetAdminAiMonitoringUseCase {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String SEOUL_ZONE_ID = "Asia/Seoul";

    private final AdminAiMonitoringQueryRepository repository;
    private final Clock clock;

    AdminAiMonitoringQueryService(AdminAiMonitoringQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiMonitoringResponse getAdminAiMonitoring(GetAdminAiMonitoringQuery query) {
        requireValidQuery(query);
        requireAuthorizedMonitoringRole(query.memberRole(), query.adminRole());

        LocalDate today = LocalDate.now(clock.withZone(SEOUL_ZONE));
        LocalDate fromDate = parseDate(query.from(), "from", today);
        LocalDate toDate = parseDate(query.to(), "to", today);
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from must not be after to");
        }

        OffsetDateTime fromAt = fromDate.atStartOfDay(SEOUL_ZONE).toOffsetDateTime();
        OffsetDateTime toAtExclusive = toDate.plusDays(1).atStartOfDay(SEOUL_ZONE).toOffsetDateTime();
        LocalDateTime fromCreatedAt = fromDate.atStartOfDay();
        LocalDateTime toCreatedAtExclusive = toDate.plusDays(1).atStartOfDay();

        AdminAiMonitoringQueryRepository.Summary summary = repository.summarize(
                new AdminAiMonitoringQueryRepository.Filter(
                        fromAt,
                        toAtExclusive,
                        fromCreatedAt,
                        toCreatedAtExclusive
                )
        );

        return toResponse(fromDate, toDate, summary);
    }

    private static AdminAiMonitoringResponse toResponse(
            LocalDate fromDate,
            LocalDate toDate,
            AdminAiMonitoringQueryRepository.Summary summary
    ) {
        AdminAiMonitoringQueryRepository.GenerationJobCounts jobs = summary.generationJobs();
        AdminAiMonitoringQueryRepository.ValidationCounts validation = summary.validation();
        AdminAiMonitoringQueryRepository.BatchRunCounts batchRuns = summary.batchRuns();
        return new AdminAiMonitoringResponse(
                new AdminAiMonitoringResponse.Period(fromDate, toDate, SEOUL_ZONE_ID),
                new AdminAiMonitoringResponse.GenerationJobs(
                        jobs.queued(),
                        jobs.running(),
                        jobs.succeeded(),
                        jobs.failed()
                ),
                new AdminAiMonitoringResponse.Validation(
                        validation.waitingAssets(),
                        validation.passCount(),
                        validation.failCount(),
                        validation.needsReviewCount(),
                        summary.failureReasons().stream()
                                .map(reason -> new AdminAiMonitoringResponse.FailureReason(
                                        reason.resultCode(),
                                        reason.count()
                                ))
                                .toList()
                ),
                new AdminAiMonitoringResponse.BatchRuns(
                        batchRuns.succeeded(),
                        batchRuns.partialFailed(),
                        batchRuns.failed(),
                        summary.latestBatchFailures().stream()
                                .map(AdminAiMonitoringQueryService::toBatchFailure)
                                .toList()
                ),
                new AdminAiMonitoringResponse.Qa(0, 0, 0, 0, List.of()),
                summary.checklists().stream()
                        .map(AdminAiMonitoringQueryService::toChecklist)
                        .toList()
        );
    }

    private static AdminAiMonitoringResponse.BatchRunFailure toBatchFailure(
            AdminAiMonitoringQueryRepository.BatchFailureRow row
    ) {
        return new AdminAiMonitoringResponse.BatchRunFailure(
                row.id(),
                row.batchName().name(),
                row.status().name(),
                row.errorType(),
                row.errorMessage(),
                row.createdAt().atZone(SEOUL_ZONE).toOffsetDateTime()
        );
    }

    private static AdminAiMonitoringResponse.Checklist toChecklist(
            AdminAiMonitoringQueryRepository.ChecklistRow row
    ) {
        return new AdminAiMonitoringResponse.Checklist(
                row.checklistType().name(),
                row.activeVersion(),
                row.totalCount() == 0 ? 0.0 : (double) row.passCount() / row.totalCount()
        );
    }

    private static void requireValidQuery(GetAdminAiMonitoringQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
    }

    private static void requireAuthorizedMonitoringRole(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole)
                || !List.of("OPERATOR", "REVIEWER", "SUPER_ADMIN").contains(adminRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static LocalDate parseDate(String value, String fieldName, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be yyyy-MM-dd");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
    }
}
