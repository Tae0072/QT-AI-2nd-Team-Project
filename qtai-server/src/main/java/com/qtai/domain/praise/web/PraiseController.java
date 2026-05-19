package com.qtai.domain.praise.web;

/**
 * 찬양 REST 엔드포인트. base path: /api/v1/praise
 *
 * 엔드포인트:
 *   GET  /              → 찬양 목록 (전체 회원)
 *   POST /              → 찬양 등록 (ADMIN only, @PreAuthorize)
 */
// TODO: @RestController, @RequestMapping("/api/v1/praise"), @RequiredArgsConstructor
public class PraiseController {

    // TODO: CreatePraiseUseCase, ListPraiseUseCase 주입
    // TODO: GET "/" — keyword/Pageable 파라미터
    // TODO: POST "/" — @PreAuthorize("hasRole('ADMIN')") + @AuthenticationPrincipal adminId
}
