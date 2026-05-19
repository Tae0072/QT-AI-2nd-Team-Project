package com.qtai.domain.admin.web;

/**
 * 관리자 REST 엔드포인트. base path: /api/v1/admin
 *
 * 모든 엔드포인트는 ROLE_ADMIN 필수 — Spring Security의 @PreAuthorize로 강제.
 * 일반 회원 토큰으로 접근 시 403 FORBIDDEN.
 *
 * 엔드포인트:
 *   GET  /stats                       → 운영 통계
 *   GET  /members                     → 회원 검색 (status/keyword/page)
 *   GET  /members/{id}                → 회원 상세
 *   POST /content/{type}/{id}/hide    → 콘텐츠 숨김
 *   POST /content/{type}/{id}/delete  → 콘텐츠 삭제
 *   POST /reports/{id}/dismiss        → 신고 반려
 */
// TODO: @RestController, @RequestMapping("/api/v1/admin"), @RequiredArgsConstructor
// TODO: 클래스 레벨 @PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    // TODO: GetStatsUseCase, LookupMemberUseCase, ModerateContentUseCase 주입
    // TODO: 각 엔드포인트 핸들러 메서드 — @AuthenticationPrincipal로 adminId 추출
    // TODO: 응답은 ResponseEntity<ApiResponse<T>>로 통일
}
