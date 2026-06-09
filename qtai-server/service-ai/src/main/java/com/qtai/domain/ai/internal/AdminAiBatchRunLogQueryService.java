package com.qtai.domain.ai.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogItem;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;

@Service
class AdminAiBatchRunLogQueryService implements ListAdminAiBatchRunLogsUseCase {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String SORT = "createdAt,desc,id,desc";
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAiBatchRunLogQueryRepository repository;

    AdminAiBatchRunLogQueryService(AdminAiBatchRunLogQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiBatchRunLogListResponse listAdminAiBatchRunLogs(ListAdminAiBatchRunLogsQuery query) {
        requireValidBaseQuery(query);
        requireAuthorizedMonitoringRole(query.memberRole(), query.adminRole());

        AiBatchName batchName = parseEnum(AiBatchName.class, query.batchName(), "batchName");
        AiBatchRunStatus status = parseEnum(AiBatchRunStatus.class, query.status(), "status");
        LocalDate fromDate = parseDate(query.from(), "from");
        LocalDate toDate = parseDate(query.to(), "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from must not be after to");
        }
        LocalDateTime fromCreatedAt = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toCreatedAtExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage page = repository.findAll(
                new AdminAiBatchRunLogQueryRepository.Filter(batchName, status, fromCreatedAt, toCreatedAtExclusive),
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        int totalPages = totalPages(page.totalElements(), query.size());

        return new AdminAiBatchRunLogListResponse(
                page.content().stream()
                        .map(AdminAiBatchRunLogQueryService::toItem)
                        .toList(),
                query.page(),
                query.size(),
                page.totalElements(),
                totalPages,
                query.page() == 0,
                totalPages == 0 || query.page() >= totalPages - 1,
                SORT
        );
    }

    private static AdminAiBatchRunLogItem toItem(AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogRow row) {
        return new AdminAiBatchRunLogItem(
                row.id(),
                row.batchName().name(),
                row.status().name(),
                row.createdCount(),
                row.failedCount(),
                row.processedCount(),
                row.errorType(),
                row.errorMessage(),
                row.startedAt(),
                row.finishedAt(),
                row.createdAt().atZone(SEOUL_ZONE).toOffsetDateTime()
        );
    }

    private static void requireValidBaseQuery(ListAdminAiBatchRunLogsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        if (query.page() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static void requireAuthorizedMonitoringRole(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole)
                || !List.of("OPERATOR", "REVIEWER", "SUPER_ADMIN").contains(adminRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }

    private static LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be yyyy-MM-dd");
        }
    }

    private static int totalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
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
