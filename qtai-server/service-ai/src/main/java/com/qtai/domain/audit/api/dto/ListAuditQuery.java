package com.qtai.domain.audit.api.dto;

public record ListAuditQuery(
        Long adminId,
        String memberRole,
        String adminRole,
        String actorType,
        Long actorId,
        String actionType,
        String targetType,
        Long targetId,
        String from,
        String to,
        int page,
        int size
) {
}
