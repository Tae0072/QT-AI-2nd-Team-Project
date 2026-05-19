package com.qtai.domain.qt.web;

/**
 * QT REST 엔드포인트. base path: /api/v1/qt
 *
 * 엔드포인트:
 *   POST   /        → 작성    (CreateQtUseCase)
 *   GET    /{id}    → 단건 조회 (GetQtUseCase)
 *   GET    /my      → 내 목록  (ListMyQtUseCase) — Pageable
 *   PUT    /{id}    → 수정    (UpdateQtUseCase)
 *   DELETE /{id}    → 삭제    (DeleteQtUseCase)
 */
// TODO: @RestController, @RequestMapping("/api/v1/qt"), @RequiredArgsConstructor
public class QtController {

    // TODO: 5개 UseCase 주입
    // TODO: 각 핸들러 @AuthenticationPrincipal memberId + ResponseEntity<ApiResponse<T>>
}
