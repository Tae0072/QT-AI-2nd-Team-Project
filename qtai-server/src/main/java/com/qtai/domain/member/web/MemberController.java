package com.qtai.domain.member.web;

/**
 * 회원 REST 엔드포인트. base path: /api/v1/members
 *
 * 엔드포인트:
 *   POST   /login         → 카카오 로그인 (인증 불필요)
 *   POST   /logout        → 로그아웃
 *   GET    /me            → 내 정보 조회
 *   GET    /{id}          → 다른 회원 정보 조회 (공개 항목만)
 *   PATCH  /me            → 내 프로필 수정
 *   DELETE /me            → 회원 탈퇴
 */
// TODO: @RestController, @RequestMapping("/api/v1/members"), @RequiredArgsConstructor
public class MemberController {

    // TODO: 5개 UseCase 주입

    // TODO: /login, /logout는 SecurityConfig에서 permitAll
    // TODO: /me, /{id}, PATCH /me, DELETE /me는 @AuthenticationPrincipal로 memberId 추출
    // TODO: 응답은 ResponseEntity<ApiResponse<T>>로 통일
}
