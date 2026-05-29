package com.qtai.domain.report.api;

import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;

/**
 * 신고 생성 UseCase 포트.
 *
 * <p>신고 대상은 (targetType, targetId) 쌍으로 식별한다(나눔글/댓글/AI Q&amp;A/AI 산출물).
 * 동일 신고자+동일 대상 중복 신고는 차단한다(DUPLICATE_REPORT, 409).
 */
public interface CreateReportUseCase {

    /**
     * 신고를 접수한다.
     *
     * @param memberId 신고자(인증된 회원) ID
     * @param request  신고 대상/사유
     * @return 접수된 신고의 식별자·상태·생성시각
     */
    ReportResponse createReport(Long memberId, ReportCreateRequest request);
}
