package com.qtai.domain.audit.web;

/**
 * 감사 REST 엔드포인트. base path: /api/v1/audit
 *
 * ROLE_ADMIN 필수 — 일반 사용자가 자신의 로그를 직접 조회하는 기능은 제공하지 않는다.
 *
 * 엔드포인트:
 *   GET /  → 감사 로그 목록 조회 (필터 + 페이징)
 */
// TODO: @RestController, @RequestMapping("/api/v1/audit"), @RequiredArgsConstructor
// TODO: @PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    // TODO: final ListAuditUseCase listAuditUseCase;

    // TODO: GET "/" — @RequestParam actorId/action/from/to + Pageable
    //       → ApiResponse.success(Page<AuditLogResponse>)
}
