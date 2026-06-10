package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
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
 *   <li>대상 존재성: 사용자용 조회 api가 있는 대상만 검증(현재 POST). 없거나 비가시면 REPORT_TARGET_NOT_FOUND.</li>
 *   <li>중복 신고 차단: (reporter, targetType, targetId) UNIQUE + TOCTOU 방어.</li>
 *   <li>접수 상태는 항상 RECEIVED. 상태 전이(REVIEWING/RESOLVED/REJECTED)는 admin 도메인 책임.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService implements CreateReportUseCase {

    private final ReportRepository reportRepository;
    private final Clock clock;
    private final GetSharingPostUseCase getSharingPostUseCase;

    @Override
    @Transactional
    public ReportResponse createReport(Long memberId, ReportCreateRequest request) {
        ReportTargetType targetType = parseTargetType(request.targetType());
        validateTargetExists(memberId, targetType, request.targetId());

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

    /**
     * 신고 대상의 존재(및 신고자 가시성)를 대상 도메인의 api/UseCase로 검증한다.
     *
     * <p>현재 검증 가능한 대상은 POST(나눔글)뿐이다(sharing {@code GetSharingPostUseCase}). 나머지는
     * 사용자용 존재 확인 수단이 없어 형식·중복 검증까지만 수행한다:
     * <ul>
     *   <li>AI_QA_REQUEST — ai {@code GetAiQaResultUseCase} 구현 빈 미등록(스텁). 구현 후 검증 추가.</li>
     *   <li>COMMENT — sharing CommentUseCase 미구현.</li>
     *   <li>AI_ASSET — ai 사용자용 단건 조회 api 미제공(관리자 전용만 존재).</li>
     * </ul>
     */
    private void validateTargetExists(Long memberId, ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.POST) {
            try {
                getSharingPostUseCase.getDetail(memberId, targetId);
            } catch (BusinessException e) {
                // 존재하지 않거나 비가시(HIDDEN/DELETE 포함)면 신고 대상으로 부적합 → 404로 통일.
                if (e.getErrorCode() == ErrorCode.SHARING_POST_NOT_FOUND) {
                    throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
                }
                throw e;
            }
        }
        // COMMENT / AI_QA_REQUEST / AI_ASSET: 사용자용 존재 확인 api 미제공/미구현 — 후속 보강 대상.
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
