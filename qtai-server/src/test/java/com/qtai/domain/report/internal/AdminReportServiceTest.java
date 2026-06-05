package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * AdminReportService 단위 테스트 — 목록 필터·처리(resolve/reject)·예외.
 */
class AdminReportServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-29T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 0, 0);

    private ReportRepository reportRepository;
    private com.qtai.domain.sharing.api.HideSharingPostForModerationUseCase hideUseCase;
    private com.qtai.domain.notification.api.SendNotificationUseCase sendNotificationUseCase;
    private com.qtai.domain.audit.api.WriteAuditLogUseCase writeAuditLogUseCase;
    private AdminReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        hideUseCase = Mockito.mock(com.qtai.domain.sharing.api.HideSharingPostForModerationUseCase.class);
        sendNotificationUseCase = Mockito.mock(com.qtai.domain.notification.api.SendNotificationUseCase.class);
        writeAuditLogUseCase = Mockito.mock(com.qtai.domain.audit.api.WriteAuditLogUseCase.class);
        service = new AdminReportService(
                reportRepository, hideUseCase, sendNotificationUseCase, writeAuditLogUseCase, FIXED_CLOCK);
    }

    private Report report(Long id, ReportTargetType type, Long targetId) {
        Report r = Report.builder()
                .reporterMemberId(1L).targetType(type).targetId(targetId)
                .reason("SPAM").detail("부적절").createdAt(NOW).build();
        setId(r, id);
        return r;
    }

    @Test
    void listReports_상태_대상필터_적용_매핑() {
        Report r = report(900L, ReportTargetType.POST, 300L);
        when(reportRepository.findForAdmin(eq(ReportStatus.RECEIVED), eq(ReportTargetType.POST), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        AdminReportListResponse res = service.listReports(
                new AdminReportListQuery("RECEIVED", "POST", 0, 20));

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).id()).isEqualTo(900L);
        assertThat(res.content().get(0).status()).isEqualTo("RECEIVED");
        assertThat(res.content().get(0).targetType()).isEqualTo("POST");
        assertThat(res.totalElements()).isEqualTo(1);
    }

    @Test
    void listReports_필터_null이면_전체조회() {
        when(reportRepository.findForAdmin(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AdminReportListResponse res = service.listReports(new AdminReportListQuery("", "  ", 0, 20));

        assertThat(res.content()).isEmpty();
    }

    @Test
    void listReports_잘못된_상태값_INVALID_INPUT() {
        assertThatThrownBy(() -> service.listReports(new AdminReportListQuery("BAD", null, 0, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void resolve_상태_RESOLVED_처리자_시각_기록() {
        Report r = report(900L, ReportTargetType.POST, 300L);
        when(reportRepository.findById(900L)).thenReturn(Optional.of(r));

        ProcessReportResult result = service.resolve(
                new ProcessReportCommand(9L, 900L, "HIDE_TARGET", "정책 위반", true));

        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.processedByAdminId()).isEqualTo(9L);
        assertThat(result.processedAt()).isNotNull();
        assertThat(r.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolve_HIDE_TARGET이면_POST_대상_숨김_알림_감사기록까지_수행() {
        Report r = report(900L, ReportTargetType.POST, 300L);
        when(reportRepository.findById(900L)).thenReturn(Optional.of(r));

        service.resolve(new ProcessReportCommand(9L, 900L, "HIDE_TARGET", "정책 위반", true));

        // ① 대상 숨김
        Mockito.verify(hideUseCase).hideForModeration(300L);
        // ② 신고자 알림 — 멱등 eventKey 포함
        var notificationCaptor = org.mockito.ArgumentCaptor.forClass(
                com.qtai.domain.notification.api.dto.NotificationSendRequest.class);
        Mockito.verify(sendNotificationUseCase).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().memberId()).isEqualTo(1L); // 신고자
        assertThat(notificationCaptor.getValue().type()).isEqualTo("REPORT_RESULT");
        assertThat(notificationCaptor.getValue().eventKey()).isEqualTo("REPORT_RESULT:900");
        // ③ 감사 기록 — adminUserId/액션/대상
        var auditCaptor = org.mockito.ArgumentCaptor.forClass(
                com.qtai.domain.audit.api.dto.AuditLogWriteRequest.class);
        Mockito.verify(writeAuditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().adminUserId()).isEqualTo(9L);
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("REPORT_RESOLVE");
        assertThat(auditCaptor.getValue().targetType()).isEqualTo("REPORT");
        assertThat(auditCaptor.getValue().targetId()).isEqualTo(900L);
    }

    @Test
    void resolve_알림_실패해도_처리와_숨김은_유지된다() {
        Report r = report(900L, ReportTargetType.POST, 300L);
        when(reportRepository.findById(900L)).thenReturn(Optional.of(r));
        Mockito.doThrow(new RuntimeException("notification down"))
                .when(sendNotificationUseCase).send(any());

        ProcessReportResult result = service.resolve(
                new ProcessReportCommand(9L, 900L, "HIDE_TARGET", "정책 위반", true));

        assertThat(result.status()).isEqualTo("RESOLVED");
        Mockito.verify(hideUseCase).hideForModeration(300L);
        Mockito.verify(writeAuditLogUseCase).write(any());
    }

    @Test
    void reject_상태_REJECTED() {
        Report r = report(901L, ReportTargetType.AI_QA_REQUEST, 700L);
        when(reportRepository.findById(901L)).thenReturn(Optional.of(r));

        ProcessReportResult result = service.reject(
                new ProcessReportCommand(9L, 901L, "NONE", "신고 사유 부적합", false));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(r.getStatus()).isEqualTo(ReportStatus.REJECTED);
        // 반려는 대상 숨김 없음 + notifyReporter=false라 알림 없음, 감사 기록은 남는다
        Mockito.verifyNoInteractions(hideUseCase);
        Mockito.verifyNoInteractions(sendNotificationUseCase);
        var auditCaptor = org.mockito.ArgumentCaptor.forClass(
                com.qtai.domain.audit.api.dto.AuditLogWriteRequest.class);
        Mockito.verify(writeAuditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("REPORT_REJECT");
    }

    @Test
    void process_없는_신고_REPORT_NOT_FOUND() {
        when(reportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(new ProcessReportCommand(9L, 999L, null, null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void process_이미_종결된_신고_REPORT_ALREADY_PROCESSED() {
        Report r = report(900L, ReportTargetType.POST, 300L);
        r.process(5L, ReportStatus.RESOLVED, NOW); // 이미 종결
        when(reportRepository.findById(900L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.reject(new ProcessReportCommand(9L, 900L, null, null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_ALREADY_PROCESSED);
    }

    private void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
