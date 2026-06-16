package com.qtai.domain.report.internal;

import com.qtai.domain.report.api.MemberReportStatsUseCase;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 상세용 신고 통계 서비스.
 *
 * <p>admin 회원 관리가 회원 단위 신고 지표(신고한 횟수/받은 신고)를 조회할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportStatsService implements MemberReportStatsUseCase {

    private final ReportRepository reportRepository;

    @Override
    public long countFiledBy(Long memberId) {
        if (memberId == null) {
            return 0L;
        }
        return reportRepository.countByReporterMemberId(memberId);
    }

    @Override
    public long countReceivedForTargets(String targetType, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return 0L;
        }
        ReportTargetType type;
        try {
            type = ReportTargetType.valueOf(targetType);
        } catch (RuntimeException e) {
            return 0L;
        }
        return reportRepository.countByTargetTypeAndTargetIdIn(type, targetIds);
    }
}
