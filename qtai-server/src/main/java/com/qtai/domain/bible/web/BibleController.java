package com.qtai.domain.bible.web;

/**
 * 성경 REST 엔드포인트. base path: /api/v1/bible
 *
 * 인증: 일부 엔드포인트는 비로그인 접근 허용 가능 (정책 따라).
 *
 * 엔드포인트:
 *   GET /books                                   → 책 목록
 *   GET /books/{book}/chapters                   → 장 목록
 *   GET /verses/{id}                             → 절 단건 조회
 *   GET /verses?book=&chapter=&start=&end=       → 절 범위 조회
 *   POST /search                                 → 키워드 검색 (페이징)
 */
// TODO: @RestController, @RequestMapping("/api/v1/bible"), @RequiredArgsConstructor
public class BibleController {

    // TODO: GetBibleVerseUseCase, ListChaptersUseCase, SearchBibleUseCase 주입

    // TODO: 5개 엔드포인트 핸들러 구현 — ApiResponse.success(...)로 통일
}
