package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.GetReportUseCase;
import com.qtai.domain.report.api.ListAdminReportsUseCase;
import com.qtai.domain.report.api.ProcessReportUseCase;
import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import com.qtai.domain.report.api.dto.ReportForEvaluation;
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
public class AdminReportService implements ListAdminReportsUseCase, ProcessReportUseCase, GetReportUseCase {

    private final ReportRepository reportRepository;
    // 후속 조치 포트 — 타 도메인은 api/UseCase로만 (CLAUDE.md §4)
    private final com.qtai.domain.sharing.api.HideSharingPostForModerationUseCase hideSharingPostForModerationUseCase;
    private final com.qtai.domain.notification.api.SendNotificationUseCase sendNotificationUseCase;
    private final com.qtai.domain.audit.api.WriteAuditLogUseCase writeAuditLogUseCase;
    private final Clock clock;

    @Override
    public ReportForEvaluation getReportForEvaluation(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        return new ReportForEvaluation(
                report.getId(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus().name(),
                report.getReporterMemberId());
    }

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

    /** RESOLVED 처리에서 대상 숨김을 의미하는 후속 조치 액션. */
    static final String ACTION_HIDE_TARGET = "HIDE_TARGET";

    private ProcessReportResult process(ProcessReportCommand command, ReportStatus newStatus) {
        Report report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (report.isClosed()) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        String previousStatus = report.getStatus().name();
        LocalDateTime now = LocalDateTime.now(clock);
        report.process(command.adminId(), newStatus, now);

        // ① 대상 숨김 — RESOLVED + HIDE_TARGET + POST 대상일 때 같은 트랜잭션에서 처리(정합 보장).
        //    COMMENT/AI 대상 숨김 연계는 해당 도메인 포트 신설 시 확장한다.
        if (newStatus == ReportStatus.RESOLVED
                && ACTION_HIDE_TARGET.equals(command.action())
                && report.getTargetType() == ReportTargetType.POST) {
            hideSharingPostForModerationUseCase.hideForModeration(report.getTargetId());
        }

        // ② 신고자 알림 — 실패가 처리 자체를 막지 않도록 분리(eventKey 멱등으로 재시도 안전).
        if (command.notifyReporter()) {
            notifyReporter(report, newStatus);
        }

        // ③ 감사 기록 — 관리자 행위 추적(CLAUDE.md §5). adminId는 admin_users.id.
        writeAuditLogUseCase.write(new com.qtai.domain.audit.api.dto.AuditLogWriteRequest(
                command.adminId(),
                "ADMIN",
                command.adminId(),
                "ADMIN:" + command.adminId(),
                newStatus == ReportStatus.RESOLVED ? "REPORT_RESOLVE" : "REPORT_REJECT",
                "REPORT",
                report.getId(),
                "{\"status\":\"" + previousStatus + "\"}",
                "{\"status\":\"" + newStatus.name() + "\",\"action\":\""
                        + (command.action() == null ? "" : command.action()) + "\"}"
        ));

        log.info("신고 처리: reportId={}, adminUserId={}, status={}, action={}, notifyReporter={}",
                report.getId(), command.adminId(), newStatus, command.action(), command.notifyReporter());

        return new ProcessReportResult(
                report.getId(), report.getStatus().name(),
                report.getProcessedByAdminId(), report.getProcessedAt());
    }

    /** 신고자에게 처리 결과 알림 — 실패는 경고 로그만 남기고 처리 흐름을 막지 않는다. */
    private void notifyReporter(Report report, ReportStatus newStatus) {
        try {
            String body = newStatus == ReportStatus.RESOLVED
                    ? "접수하신 신고가 처리되었습니다."
                    : "검토 결과 조치 대상이 아닌 것으로 확인되었습니다.";
            sendNotificationUseCase.send(new com.qtai.domain.notification.api.dto.NotificationSendRequest(
                    report.getReporterMemberId(),
                    "REPORT_RESULT",
                    "신고 처리 결과 안내",
                    body,
                    null,
                    "REPORT",
                    report.getId(),
                    "REPORT_RESULT:" + report.getId()
            ));
        } catch (RuntimeException exception) {
            log.warn("신고자 알림 발송 실패(처리 자체는 유지). reportId={}, errorType={}, errorMessage={}",
                    report.getId(), exception.getClass().getSimpleName(), exception.getMessage());
        }
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
