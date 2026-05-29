package com.qtai.domain.report.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 신고 영속성 포트. Spring Data JPA로 구현.
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 동일 신고자가 동일 대상을 이미 신고했는지 검사 — 중복 신고 차단용.
     * uk_reports_reporter_target 와 동일한 키 조합.
     */
    boolean existsByReporterMemberIdAndTargetTypeAndTargetId(
            Long reporterMemberId, ReportTargetType targetType, Long targetId);
}
