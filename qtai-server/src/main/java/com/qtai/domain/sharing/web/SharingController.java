package com.qtai.domain.sharing.web;

/**
 * 공유 REST 엔드포인트. base path: /api/v1/shares
 *
 * 엔드포인트:
 *   POST   /                  → 공유 생성 (로그인 필요)
 *   GET    /token/{token}     → 스냅샷 조회 (비로그인 가능)
 *   DELETE /{id}              → 공유 취소 (owner only)
 */
// TODO: @RestController, @RequestMapping("/api/v1/shares"), @RequiredArgsConstructor
public class SharingController {

    // TODO: CreateShareUseCase, GetSharedSnapshotUseCase, RevokeShareUseCase 주입
    // TODO: GET /token/{token}은 SecurityConfig에서 permitAll
    // TODO: 나머지는 @AuthenticationPrincipal memberId + ApiResponse 포장
}
