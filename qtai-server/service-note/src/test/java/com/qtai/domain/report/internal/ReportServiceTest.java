package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

/**
 * 신고 접수 서비스 단위 테스트.
 *
 * <p>service-note는 신고 접수만 담당한다. POST/COMMENT 대상은 즉시 검증하고, AI_QA_REQUEST 대상 검증은
 * service-ai 조회 API가 준비된 뒤 설정으로 활성화한다.
 */
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
        return reportService(true);
    }

    private ReportService reportService(boolean aiQaRequestValidationEnabled) {
        ReportTargetValidationProperties properties = new ReportTargetValidationProperties();
        properties.setAiQaRequestEnabled(aiQaRequestValidationEnabled);
        return new ReportService(
                reportRepository,
                CLOCK,
                getSharingPostUseCase,
                checkCommentExistsUseCase,
                checkAiQaRequestExistsClient,
                properties);
    }

    @Test
    void 지원하지_않는_대상타입이면_INVALID_INPUT() {
        ReportCreateRequest request = new ReportCreateRequest("UNKNOWN", 1L, "SPAM", null);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 중복_신고면_DUPLICATE_REPORT() {
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
    void POST_대상이_없으면_REPORT_TARGET_NOT_FOUND() {
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
    void COMMENT_대상이_없으면_REPORT_TARGET_NOT_FOUND() {
        ReportCreateRequest request = new ReportCreateRequest("COMMENT", 100L, "SPAM", null);
        when(checkCommentExistsUseCase.existsReportableComment(100L)).thenReturn(false);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void AI_QA_REQUEST_검증_활성화_상태에서_대상이_없으면_REPORT_TARGET_NOT_FOUND() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", null);
        when(checkAiQaRequestExistsClient.exists(1L, 9L)).thenReturn(false);

        assertThatThrownBy(() -> reportService().createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void COMMENT_신고면_RECEIVED_상태로_저장된다() {
        ReportCreateRequest request = new ReportCreateRequest("COMMENT", 100L, "SPAM", "상세");
        when(checkCommentExistsUseCase.existsReportableComment(100L)).thenReturn(true);
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.COMMENT, 100L)).thenReturn(false);

        ReportResponse response = reportService().createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void AI_QA_REQUEST_검증_활성화_상태에서_신고면_RECEIVED_상태로_저장된다() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", "상세");
        when(checkAiQaRequestExistsClient.exists(1L, 9L)).thenReturn(true);
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_QA_REQUEST, 9L)).thenReturn(false);

        ReportResponse response = reportService().createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void AI_QA_REQUEST_검증_비활성화면_존재확인_없이_저장된다() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", "상세");
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_QA_REQUEST, 9L)).thenReturn(false);

        ReportResponse response = reportService(false).createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verifyNoInteractions(checkAiQaRequestExistsClient);
        verify(reportRepository).save(any(Report.class));
    }
}
