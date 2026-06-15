package com.qtai.domain.audit.internal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogItem;
import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;

@Service
class AuditQueryService implements ListAuditUseCase {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String SORT = "createdAt,desc,id,desc";
    private static final String AI_ASSET_TARGET_TYPE = "AI_GENERATED_ASSET";
    private static final String QT_PASSAGE_TARGET_TYPE = "QT_PASSAGE";
    private static final String SOURCE_VIDEO_TARGET_TYPE = "SOURCE_VIDEO";
    private static final String QT_VIDEO_CLIP_TARGET_TYPE = "QT_VIDEO_CLIP";
    // 감사 조회가 허용하는 대상 유형 — AI 산출물 + 관리자 해설 생성 트리거(QT 본문 대상).
    private static final List<String> ALLOWED_TARGET_TYPES = List.of(
            AI_ASSET_TARGET_TYPE, QT_PASSAGE_TARGET_TYPE, "REPORT", "NOTICE", "MUSIC_TRACK",
            SOURCE_VIDEO_TARGET_TYPE, QT_VIDEO_CLIP_TARGET_TYPE);
    // AD-07 admin audit allowlist (deny-by-default). Operational admin actions exposed;
    // sensitive AI governance (VALIDATION_REFERENCE_JOB / evaluation / prompt) intentionally excluded.
    private static final List<String> ADMIN_AUDIT_ACTION_TYPES = List.of(
            "AI_ASSET_APPROVE", "AI_ASSET_REJECT", "AI_ASSET_HIDE",
            "AI_REGENERATE_REQUEST", "AI_EXPLANATION_GENERATE_REQUEST", "SIMULATOR_CLIP_HIDE",
            "REPORT_RESOLVE", "REPORT_REJECT",
            "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_PUBLISH", "NOTICE_HIDE",
            "MUSIC_TRACK_CREATE", "MUSIC_TRACK_UPDATE", "MUSIC_TRACK_HIDE", "MUSIC_TRACK_PUBLISH", "MUSIC_TRACK_DELETE",
            "QT_PASSAGE_CREATE", "QT_PASSAGE_UPDATE", "QT_PASSAGE_HIDE", "QT_PASSAGE_PUBLISH",
            "QT_VIDEO_SOURCE_CREATE", "QT_VIDEO_SOURCE_UPDATE", "QT_VIDEO_SEGMENTS_REPLACE",
            "QT_VIDEO_CLIP_PREPARE", "QT_VIDEO_CLIP_STATUS_CHANGE"
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditQueryRepository repository;

    AuditQueryService(AuditQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogListResponse listAuditLogs(ListAuditQuery query) {
        requireValidBaseQuery(query);
        requireAuthorizedAuditRole(query.memberRole(), query.adminRole());

        String targetType = resolveTargetType(query.targetType());
        List<String> actionTypes = resolveActionTypes(query.actionType());
        LocalDate fromDate = parseDate(query.from(), "from");
        LocalDate toDate = parseDate(query.to(), "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from must not be after to");
        }
        OffsetDateTime fromCreatedAt = fromDate == null
                ? null
                : fromDate.atStartOfDay(SEOUL_ZONE).toOffsetDateTime();
        OffsetDateTime toCreatedAtExclusive = toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay(SEOUL_ZONE).toOffsetDateTime();

        AuditQueryRepository.AuditLogPage page = repository.findAll(
                new AuditQueryRepository.Filter(
                        blankToNull(query.actorType()),
                        query.actorId(),
                        actionTypes,
                        targetType,
                        query.targetId(),
                        fromCreatedAt,
                        toCreatedAtExclusive
                ),
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        int totalPages = totalPages(page.totalElements(), query.size());

        return new AuditLogListResponse(
                page.content().stream()
                        .map(AuditQueryService::toItem)
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

    private static AuditLogItem toItem(AuditQueryRepository.AuditLogRow row) {
        return new AuditLogItem(
                row.id(),
                row.adminUserId(),
                row.actorType(),
                row.actorId(),
                row.actorLabel(),
                row.actionType(),
                row.targetType(),
                row.targetId(),
                row.beforeJson(),
                row.afterJson(),
                row.createdAt().atZoneSameInstant(SEOUL_ZONE).toOffsetDateTime()
        );
    }

    private static void requireValidBaseQuery(ListAuditQuery query) {
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

    private static void requireAuthorizedAuditRole(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole)
                || !List.of("OPERATOR", "REVIEWER", "SUPER_ADMIN").contains(adminRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static List<String> resolveActionTypes(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return ADMIN_AUDIT_ACTION_TYPES;
        }
        if (!ADMIN_AUDIT_ACTION_TYPES.contains(actionType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "actionType is not supported");
        }
        return List.of(actionType);
    }

    private static String resolveTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            // 대상 미지정 — AI 액션 전체를 대상 무관 조회(AI 산출물 + QT 본문 트리거 포함).
            // 리포지토리 LIST_WHERE 가 targetType null 을 "필터 없음"으로 처리한다.
            return null;
        }
        if (!ALLOWED_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetType is not supported");
        }
        return targetType;
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
