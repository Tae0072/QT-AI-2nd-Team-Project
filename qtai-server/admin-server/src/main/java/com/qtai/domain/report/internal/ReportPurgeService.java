package com.qtai.domain.report.internal;

import com.qtai.domain.report.api.PurgeMemberReportDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * report 도메인 — 회원 보존기간 만료 정리 구현.
 * 자기 도메인 테이블(reports)에서 회원이 접수한 신고만 삭제한다.
 */
@Service
@RequiredArgsConstructor
public class ReportPurgeService implements PurgeMemberReportDataUseCase {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public int purgeByMemberId(Long memberId) {
        return jdbc.update("DELETE FROM reports WHERE reporter_member_id = ?", memberId);
    }
}
