package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 도메인 서비스.
 *
 * <p>API 명세서 §4.4.7 (POST /api/v1/reports) 기준.
 * <ul>
 *   <li>대상 식별: (targetType, targetId). targetType은 ReportTargetType enum으로 검증.</li>
 *   <li>중복 신고 차단: (reporter, targetType, targetId) UNIQUE + TOCTOU 방어.</li>
 *   <li>접수 상태는 항상 RECEIVED. 상태 전이(REVIEWING/RESOLVED/REJECTED)는 admin 도메인 책임.</li>
 * </ul>
 *
 * <p>대상 존재성(나눔글/AI 산출물 등) 교차 검증은 각 대상 도메인 client 어댑터로 후속 처리 예정이며,
 * 본 MVP 접수 경로에서는 형식 검증과 중복 차단만 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService implements CreateReportUseCase {

    private final ReportRepository reportRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ReportResponse createReport(Long memberId, ReportCreateRequest request) {
        ReportTargetType targetType = parseTargetType(request.targetType());

        if (reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                memberId, targetType, request.targetId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.builder()
                .reporterMemberId(memberId)
                .targetType(targetType)
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .createdAt(LocalDateTime.now(clock))
                .build();
        try {
            reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            // TOCTOU: existsBy 이후 동시 INSERT → uk_reports_reporter_target 위반 시 비즈니스 예외로 변환
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        log.info("신고 접수: reportId={}, reporterId={}, targetType={}, targetId={}",
                report.getId(), memberId, targetType, request.targetId());
        return toResponse(report);
    }

    private ReportTargetType parseTargetType(String raw) {
        try {
            return ReportTargetType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "지원하지 않는 신고 대상 타입입니다: " + raw);
        }
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
