package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.domain.report.client.ai.CheckAiQaRequestExistsClient;
import com.qtai.domain.sharing.api.CheckCommentExistsUseCase;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService implements CreateReportUseCase {

    private final ReportRepository reportRepository;
    private final Clock clock;
    private final GetSharingPostUseCase getSharingPostUseCase;
    private final CheckCommentExistsUseCase checkCommentExistsUseCase;
    private final CheckAiQaRequestExistsClient checkAiQaRequestExistsClient;
    private final ReportTargetValidationProperties targetValidationProperties;

    /**
     * 신고를 접수한다.
     *
     * <p>대상은 `(targetType, targetId)`로 식별하고, 대상별 공개 UseCase/client 포트로 신고 가능한 대상인지 먼저
     * 확인한다. 중복 신고는 사전 조회와 DB unique 제약 양쪽으로 막는다.
     */
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
            // TOCTOU: existsBy 이후 동시 INSERT로 unique 제약이 깨지면 동일한 비즈니스 예외로 변환한다.
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

    private void validateTargetExists(Long memberId, ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> validatePostTarget(memberId, targetId);
            case COMMENT -> validateCommentTarget(targetId);
            case AI_QA_REQUEST -> validateAiQaRequestTarget(memberId, targetId);
            case AI_ASSET -> {
                // 사용자용 AI_ASSET 존재 확인 포트는 정책 확정 후 후속 작업에서 연결한다.
            }
        }
    }

    private void validatePostTarget(Long memberId, Long targetId) {
        try {
            getSharingPostUseCase.getDetail(memberId, targetId);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.SHARING_POST_NOT_FOUND) {
                throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
            }
            throw e;
        }
    }

    private void validateCommentTarget(Long targetId) {
        if (!checkCommentExistsUseCase.existsReportableComment(targetId)) {
            throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }
    }

    private void validateAiQaRequestTarget(Long memberId, Long targetId) {
        if (!targetValidationProperties.isAiQaRequestEnabled()) {
            return;
        }
        if (!checkAiQaRequestExistsClient.exists(memberId, targetId)) {
            throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
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
