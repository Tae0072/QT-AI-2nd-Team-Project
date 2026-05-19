package com.qtai.domain.note.web;

/**
 * 노트 REST 엔드포인트. base path: /api/v1/notes
 *
 * 엔드포인트:
 *   POST   /qt/{qtId}    → 특정 QT에 노트 작성
 *   GET    /qt/{qtId}    → 특정 QT의 노트 목록
 *   GET    /{id}         → 노트 단건 조회
 *   PUT    /{id}         → 노트 수정
 *   DELETE /{id}         → 노트 삭제
 */
// TODO: @RestController, @RequestMapping("/api/v1/notes"), @RequiredArgsConstructor
public class NoteController {

    // TODO: 4개 UseCase 주입
    // TODO: 모든 엔드포인트 @AuthenticationPrincipal memberId + ApiResponse 포장
}
