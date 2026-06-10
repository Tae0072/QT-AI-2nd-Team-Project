package com.qtai.domain.audit.api.dto;

import java.time.OffsetDateTime;

public record AuditLogItem(
        Long id,
        Long adminUserId,
        String actorType,
        Long actorId,
        String actorLabel,
        String actionType,
        String targetType,
        Long targetId,
        String beforeJson,
        String afterJson,
        OffsetDateTime createdAt
) {
}
