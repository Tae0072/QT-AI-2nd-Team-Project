package com.qtai.domain.audit.api.dto;

import java.time.OffsetDateTime;

/**
 * 관리자 대시보드에 노출 가능한 sanitized 감사 로그 항목.
 */
public record AdminDashboardAuditLog(
        Long id,
        Long adminUserId,
        String actorType,
        String actionType,
        String targetType,
        Long targetId,
        OffsetDateTime createdAt
) {
}
