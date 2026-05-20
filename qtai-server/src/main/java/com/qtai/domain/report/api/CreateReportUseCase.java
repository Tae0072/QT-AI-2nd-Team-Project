package com.qtai.domain.report.api;

/**
 * 신고 생성 UseCase 포트.
 *
 * 신고 대상은 sharing 스냅샷(공유된 QT/노트/스터디 등). 자유 게시판 형태가 아닌
 * 공유된 콘텐츠 단위로만 신고 가능 → 신고 대상 범위가 명확.
 * 동일 신고자+동일 대상 중복 신고는 차단 (UNIQUE 또는 서비스 검증).
 */
public interface CreateReportUseCase {

    // TODO: ReportResponse createReport(Long memberId, ReportCreateRequest request);
    //       중복 신고 시 throw BusinessException(INVALID_INPUT, "이미 신고함")
}
