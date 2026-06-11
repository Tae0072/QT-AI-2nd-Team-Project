package com.qtai.domain.audit.api.dto;

/** 감사 로그 기록 요청 DTO. */
public record AuditLogWriteRequest(
        Long adminUserId,
        String actorType,
        Long actorId,
        String actorLabel,
        String actionType,
        String targetType,
        Long targetId,
        String beforeJson,
        String afterJson
) {
}
