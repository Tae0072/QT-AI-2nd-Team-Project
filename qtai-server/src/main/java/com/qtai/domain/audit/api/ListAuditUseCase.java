package com.qtai.domain.audit.api;

/**
 * 감사 로그 조회 UseCase 포트.
 *
 * 호출자는 admin 도메인 한정. 일반 사용자에게 노출 금지.
 */
public interface ListAuditUseCase {

    // TODO: Page<AuditLogResponse> list(Long actorId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable);
    //       필터: 행위자/액션/기간. 페이징 필수 (로그 양이 크기 때문).
}
