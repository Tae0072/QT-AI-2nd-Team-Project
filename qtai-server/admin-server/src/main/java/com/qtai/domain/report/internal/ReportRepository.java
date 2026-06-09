package com.qtai.domain.report.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 관리자 신고 목록 조회 — status·targetType 선택 필터(null이면 전체), 생성 최신순.
     */
    @Query("""
            select r from Report r
            where (:status is null or r.status = :status)
              and (:targetType is null or r.targetType = :targetType)
            order by r.createdAt desc
            """)
    Page<Report> findForAdmin(@Param("status") ReportStatus status,
                              @Param("targetType") ReportTargetType targetType,
                              Pageable pageable);
}
