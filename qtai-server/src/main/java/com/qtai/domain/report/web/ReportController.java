package com.qtai.domain.report.web;

/**
 * 신고 REST 엔드포인트. base path: /api/v1/reports
 *
 * 엔드포인트:
 *   POST /        → 신고 생성
 *   GET  /my      → 내 신고 이력 (페이징)
 *   GET  /{id}    → 신고 단건 조회 (본인 또는 ADMIN)
 */
// TODO: @RestController, @RequestMapping("/api/v1/reports"), @RequiredArgsConstructor
public class ReportController {

    // TODO: CreateReportUseCase, GetReportUseCase 주입
    // TODO: @AuthenticationPrincipal memberId + ApiResponse 포장
}
