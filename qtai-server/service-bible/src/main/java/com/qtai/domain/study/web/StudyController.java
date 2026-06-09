package com.qtai.domain.study.web;

/**
 * 스터디 REST 엔드포인트. base path: /api/v1/study
 *
 * 엔드포인트:
 *   GET /        → 스터디 목록 (keyword/category/Pageable)
 *   GET /{id}    → 스터디 상세
 *
 * 일반 회원 접근. 등록/수정 API는 별도 관리자 인터페이스에서 제공.
 */
// TODO: @RestController, @RequestMapping("/api/v1/study"), @RequiredArgsConstructor
public class StudyController {

    // TODO: GetStudyUseCase, ListStudyUseCase 주입
    // TODO: 두 엔드포인트 — ApiResponse.success(...)로 통일
}
