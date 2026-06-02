package com.qtai.domain.audit.api.dto;

import java.time.OffsetDateTime;

/**
 * 감사 로그 조회 필터. 모든 값은 선택(null이면 미적용).
 *
 * @param actorType  행위자 유형 (MEMBER / ADMIN / SYSTEM_BATCH 등)
 * @param actorId    행위자 식별자 (시스템 작업이면 null일 수 있음)
 * @param actionType 액션 유형 (CHECKLIST_CREATE 등)
 * @param from       조회 시작 시각(이상)
 * @param to         조회 종료 시각(이하)
 */
public record AuditLogFilter(
        String actorType,
        Long actorId,
        String actionType,
        OffsetDateTime from,
        OffsetDateTime to
) {}
