package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.notification.api.SendNotificationUseCase;
import com.qtai.domain.report.api.dto.ReportForEvaluation;
import com.qtai.domain.sharing.api.HideSharingPostForModerationUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private HideSharingPostForModerationUseCase hideSharingPostForModerationUseCase;
    @Mock private SendNotificationUseCase sendNotificationUseCase;
    @Mock private WriteAuditLogUseCase writeAuditLogUseCase;

    private AdminReportService service;

    @BeforeEach
    void setUp() {
        service = new AdminReportService(
                reportRepository,
                hideSharingPostForModerationUseCase,
                sendNotificationUseCase,
                writeAuditLogUseCase,
                Clock.systemDefaultZone()
        );
    }

    @Test
    void getReportForEvaluationReturnsMetadataOnly() {
        Report report = Report.builder()
                .reporterMemberId(999L)
                .targetType(ReportTargetType.AI_QA_REQUEST)
                .targetId(700L)
                .reason("FACT_ERROR")
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(report, "id", 789L);
        when(reportRepository.findById(789L)).thenReturn(Optional.of(report));

        ReportForEvaluation result = service.getReportForEvaluation(789L);

        assertThat(result.id()).isEqualTo(789L);
        assertThat(result.targetType()).isEqualTo("AI_QA_REQUEST");
        assertThat(result.targetId()).isEqualTo(700L);
        assertThat(result.reason()).isEqualTo("FACT_ERROR");
        assertThat(result.status()).isEqualTo("RECEIVED");
        assertThat(result.reporterMemberId()).isEqualTo(999L);
    }

    @Test
    void getReportForEvaluationThrowsWhenNotFound() {
        when(reportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReportForEvaluation(404L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }
}
