package com.qtai.domain.report.api;

import java.util.Collection;

/**
 * 회원 상세용 신고 통계 UseCase 포트 (관리자 회원 관리에서 호출).
 *
 * <p>도메인 경계: report 도메인 내부(Report/Repository)를 직접 노출하지 않고 집계 수치만 제공한다.
 */
public interface MemberReportStatsUseCase {

    /** 이 회원이 신고자(reporter)로서 접수한 신고 건수. */
    long countFiledBy(Long memberId);

    /**
     * 이 회원이 소유한 대상(공유글/댓글 등)이 받은 신고 건수.
     *
     * @param targetType "POST" / "COMMENT" 등 ReportTargetType 이름
     * @param targetIds  회원 소유 콘텐츠 ID 목록(비면 0 반환)
     */
    long countReceivedForTargets(String targetType, Collection<Long> targetIds);
}
