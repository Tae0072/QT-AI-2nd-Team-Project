package com.qtai.domain.audit.api.dto;

import java.time.OffsetDateTime;

/**
 * 감사 로그 조회 응답 DTO.
 *
 * <p>변경 전/후 원문(before/after json)은 목록 응답에서 제외한다(로그 양이 크고 상세 영역 자료).
 */
public record AuditLogResponse(
        Long id,
        Long adminUserId,
        String actorType,
        Long actorId,
        String actorLabel,
        String actionType,
        String targetType,
        Long targetId,
        OffsetDateTime createdAt
) {}
