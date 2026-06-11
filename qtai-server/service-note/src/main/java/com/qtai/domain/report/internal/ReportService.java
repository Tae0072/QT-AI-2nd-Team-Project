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
