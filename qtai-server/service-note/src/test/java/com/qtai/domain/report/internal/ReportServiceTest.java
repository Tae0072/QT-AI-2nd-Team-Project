package com.qtai.domain.report.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 신고 접수 서비스 단위 테스트.
 *
 * <p>service-note는 신고 "접수"만 담당한다(검수는 admin-server). POST 대상만 존재 검증을 수행하고,
 * 나머지 타입은 형식·중복 검증까지만 한다.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private GetSharingPostUseCase getSharingPostUseCase;

    private ReportService reportService() {
        return new ReportService(reportRepository, CLOCK, getSharingPostUseCase);
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
    void 정상_접수면_RECEIVED_상태로_저장된다() {
        ReportCreateRequest request = new ReportCreateRequest("AI_QA_REQUEST", 9L, "FACT_ERROR", "상세");
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_QA_REQUEST, 9L)).thenReturn(false);

        ReportResponse response = reportService().createReport(1L, request);

        assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED.name());
        verify(reportRepository).save(any(Report.class));
    }
}
