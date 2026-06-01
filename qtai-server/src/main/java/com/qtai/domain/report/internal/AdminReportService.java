package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.ListAdminReportsUseCase;
import com.qtai.domain.report.api.ProcessReportUseCase;
import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 처리 서비스.
 *
 * <p>API 명세서 §4.7.4 기준. 목록 조회 + 처리(resolve/reject = 상태 전이 + 처리자·시각 기록)를 담당한다.
 *
 * <p>범위: 본 구현은 신고 상태/처리 기록까지다. 대상 숨김(HIDE_TARGET — sharing/ai 도메인 연계),
 * 신고자 알림(notification), 감사 로그(audit_logs) 연동은 후속 과제다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService implements ListAdminReportsUseCase, ProcessReportUseCase {

    private final ReportRepository reportRepository;
    private final Clock clock;

    @Override
    public AdminReportListResponse listReports(AdminReportListQuery query) {
        ReportStatus status = parseStatus(query.status());
        ReportTargetType targetType = parseTargetType(query.targetType());

        Page<Report> page = reportRepository.findForAdmin(
                status, targetType, PageRequest.of(query.page(), query.size()));

        return new AdminReportListResponse(
                page.getContent().stream().map(this::toItem).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public ProcessReportResult resolve(ProcessReportCommand command) {
        return process(command, ReportStatus.RESOLVED);
    }

    @Override
    @Transactional
    public ProcessReportResult reject(ProcessReportCommand command) {
        return process(command, ReportStatus.REJECTED);
    }

    private ProcessReportResult process(ProcessReportCommand command, ReportStatus newStatus) {
        Report report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (report.isClosed()) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        report.process(command.adminId(), newStatus, now);

        log.info("신고 처리: reportId={}, adminId={}, status={}, action={}, notifyReporter={}",
                report.getId(), command.adminId(), newStatus, command.action(), command.notifyReporter());
        // TODO(후속): action=HIDE_TARGET 대상 숨김(sharing/ai), 신고자 알림(notification), 감사 로그(audit_logs)

        return new ProcessReportResult(
                report.getId(), report.getStatus().name(),
                report.getProcessedByAdminId(), report.getProcessedAt());
    }

    private ReportStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ReportStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 신고 상태입니다: " + raw);
        }
    }

    private ReportTargetType parseTargetType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ReportTargetType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 신고 대상 타입입니다: " + raw);
        }
    }

    private AdminReportListResponse.Item toItem(Report r) {
        return new AdminReportListResponse.Item(
                r.getId(), r.getReporterMemberId(), r.getTargetType().name(), r.getTargetId(),
                r.getReason(), r.getDetail(), r.getStatus().name(),
                r.getProcessedByAdminId(), r.getProcessedAt(), r.getCreatedAt());
    }
}
