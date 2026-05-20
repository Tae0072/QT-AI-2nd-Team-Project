package com.qtai.domain.audit.api.dto;

/** 감사 로그 조회 응답 DTO. */
public record AuditLogResponse(
        // TODO: Long id
        // TODO: Long actorId          — 행위자 (시스템 작업이면 null)
        // TODO: String action         — LOGIN / CREATE_QT / DELETE_NOTE ...
        // TODO: String targetType     — QT / NOTE / MEMBER ...
        // TODO: Long targetId
        // TODO: String metadata       — JSON 문자열 (IP, UA 등 부가 정보)
        // TODO: LocalDateTime createdAt
) {}
