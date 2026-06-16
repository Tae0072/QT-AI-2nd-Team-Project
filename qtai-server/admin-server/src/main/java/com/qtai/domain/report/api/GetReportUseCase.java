package com.qtai.domain.report.api;

import com.qtai.domain.report.api.dto.ReportForEvaluation;

/**
 * 신고 조회 UseCase 포트.
 *
 * 호출 권한: 신고자 본인 또는 ADMIN. 일반 회원은 다른 사람의 신고 조회 불가.
 */
public interface GetReportUseCase {

    /**
     * 평가 케이스 후보 등록(USER_REPORT)용 신고 메타데이터 조회.
     *
     * <p>AI 도메인이 신고→평가항목 등록 시 호출한다. 원문/상세가 아니라 식별자·메타만 반환한다.
     *
     * @throws com.qtai.common.exception.BusinessException REPORT_NOT_FOUND — 신고가 없을 때
     */
    ReportForEvaluation getReportForEvaluation(Long reportId);

    // TODO: ReportResponse getReport(Long viewerId, Long reportId);
    // TODO: Page<ReportResponse> listMy(Long memberId, Pageable pageable);  — 내가 한 신고 목록
}
