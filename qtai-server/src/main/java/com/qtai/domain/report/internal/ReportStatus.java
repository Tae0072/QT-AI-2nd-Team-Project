package com.qtai.domain.report.internal;

/**
 * 신고 처리 상태.
 *
 * <p>ERD: reports.status (VARCHAR(20), 기본값 'RECEIVED').
 * <p>접수(RECEIVED) → 검토중(REVIEWING) → 처리완료(RESOLVED)/반려(REJECTED).
 * <p>상태 전이는 관리자(admin 도메인)에서 수행한다. 사용자 신고 접수는 RECEIVED로만 생성한다.
 */
public enum ReportStatus {
    RECEIVED,
    REVIEWING,
    RESOLVED,
    REJECTED
}
