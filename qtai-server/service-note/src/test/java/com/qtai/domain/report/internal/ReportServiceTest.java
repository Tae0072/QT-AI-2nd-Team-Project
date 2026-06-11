package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.domain.report.client.ai.CheckAiQaRequestExistsClient;
import com.qtai.domain.sharing.api.CheckCommentExistsUseCase;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-10T00:00:00Z"),
            ZoneId.of("Asia/Seoul"));

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private GetSharingPostUseCase getSharingPostUseCase;
    @Mock
    private CheckCommentExistsUseCase checkCommentExistsUseCase;
    @Mock
    private CheckAiQaRequestExistsClient checkAiQaRequestExistsClient;

    private ReportService reportService() {
        return new ReportService(
                reportRepository,
                CLOCK,
                getSharingPostUseCase,
                checkCommentExistsUseCase,
                checkAiQaRequestExistsClient);
    }

    @Test
    void unsupported_target_type_returns_invalid_input() {
        ReportCreateRequest request = new ReportCreateRequest("UNKNOWN", 1L, "SPAM", null);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void duplicate_report_returns_duplicate_report() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", null);
        when(checkAiQaRequestExistsClient.exists(1L, 9L)).thenReturn(true);
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_QA_REQUEST, 9L)).thenReturn(true);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REPORT);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void post_target_not_found_maps_to_report_target_not_found() {
        ReportCreateRequest request = new ReportCreateRequest("POST", 100L, "SPAM", null);
        when(getSharingPostUseCase.getDetail(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void comment_target_not_found_maps_to_report_target_not_found() {
        ReportCreateRequest request = new ReportCreateRequest("COMMENT", 100L, "SPAM", null);
        when(checkCommentExistsUseCase.existsReportableComment(100L)).thenReturn(false);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void ai_qa_request_target_not_found_maps_to_report_target_not_found() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", null);
        when(checkAiQaRequestExistsClient.exists(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void comment_report_saves_received_status() {
        ReportCreateRequest request = new ReportCreateRequest("COMMENT", 100L, "SPAM", "상세");
        when(checkCommentExistsUseCase.existsReportableComment(100L)).thenReturn(true);
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.COMMENT, 100L)).thenReturn(false);

        ReportResponse response = reportService().createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void ai_qa_request_report_saves_received_status() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", "상세");
        when(checkAiQaRequestExistsClient.exists(1L, 9L)).thenReturn(true);
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_QA_REQUEST, 9L)).thenReturn(false);

        ReportResponse response = reportService().createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verify(reportRepository).save(any(Report.class));
    }
}
