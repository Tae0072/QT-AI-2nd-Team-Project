package com.qtai.domain.audit.api.dto;

/** 감사 로그 기록 요청 DTO. */
public record AuditLogWriteRequest(
        // TODO: Long actorId          — 행위자 ID (시스템 작업은 null 허용)
        // TODO: String action         — 액션 식별자 (도메인_동사, 예: QT_CREATE)
        // TODO: String targetType     — 대상 엔티티 타입
        // TODO: Long targetId         — 대상 엔티티 ID (없으면 null)
        // TODO: String metadata       — 추가 컨텍스트 JSON (IP, UA, before/after 등)
) {}
