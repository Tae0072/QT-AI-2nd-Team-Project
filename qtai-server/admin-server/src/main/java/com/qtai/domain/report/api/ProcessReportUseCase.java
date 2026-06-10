package com.qtai.domain.report.api;

import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;

/**
 * 관리자 신고 처리 UseCase 포트.
 *
 * <p>API 명세서 §4.7.4 (POST /api/v1/admin/reports/{reportId}/resolve|reject). OPERATOR/SUPER_ADMIN 권한.
 * <p>처리 = 상태 전이(RECEIVED/REVIEWING → RESOLVED/REJECTED) + 처리자·시각 기록.
 * <p>대상 숨김(HIDE_TARGET)·신고자 알림·감사 로그 연동은 후속 과제다(본 구현은 상태/처리기록까지).
 */
public interface ProcessReportUseCase {

    /** 신고를 처리 완료(RESOLVED)로 종결한다. */
    ProcessReportResult resolve(ProcessReportCommand command);

    /** 신고를 반려(REJECTED)로 종결한다. */
    ProcessReportResult reject(ProcessReportCommand command);
}
