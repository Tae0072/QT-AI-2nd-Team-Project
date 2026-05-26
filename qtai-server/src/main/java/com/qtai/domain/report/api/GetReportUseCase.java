package com.qtai.domain.report.api;

/**
 * 신고 조회 UseCase 포트.
 *
 * 호출 권한: 신고자 본인 또는 ADMIN. 일반 회원은 다른 사람의 신고 조회 불가.
 */
public interface GetReportUseCase {

    // TODO: ReportResponse getReport(Long viewerId, Long reportId);
    // TODO: Page<ReportResponse> listMy(Long memberId, Pageable pageable);  — 내가 한 신고 목록
}
