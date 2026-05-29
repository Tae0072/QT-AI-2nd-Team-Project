package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * ReportService 단위 테스트.
 *
 * <p>검증 범위: 신고 접수 성공(RECEIVED), 대상 존재성(POST), 중복 신고 차단(사전 검사 + TOCTOU),
 * 잘못된 대상 타입 거부.
 */
class ReportServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-29T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private ReportRepository reportRepository;
    private GetSharingPostUseCase getSharingPostUseCase;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        getSharingPostUseCase = Mockito.mock(GetSharingPostUseCase.class);
        reportService = new ReportService(reportRepository, FIXED_CLOCK, getSharingPostUseCase);
    }

    @Test
    void createReport_신고_접수_성공_RECEIVED() {
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.POST, 300L)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report saved = inv.getArgument(0);
            setId(saved, 900L);
            return saved;
        });

        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "INAPPROPRIATE", "부적절한 표현이 있습니다.");
        ReportResponse response = reportService.createReport(1L, request);

        assertThat(response.id()).isEqualTo(900L);
        assertThat(response.status()).isEqualTo("RECEIVED");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createReport_중복_신고_DUPLICATE_REPORT() {
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.POST, 300L)).thenReturn(true);

        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "SPAM", null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_REPORT);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_동시INSERT_UK위반_DUPLICATE_REPORT() {
        // AI_ASSET은 대상 존재 검증 대상이 아님(사용자 조회 api 부재) → 바로 중복/저장 경로.
        when(reportRepository.existsByReporterMemberIdAndTargetTypeAndTargetId(
                1L, ReportTargetType.AI_ASSET, 42L)).thenReturn(false);
        when(reportRepository.save(any(Report.class)))
                .thenThrow(new DataIntegrityViolationException("uk_reports_reporter_target"));

        ReportCreateRequest request =
                new ReportCreateRequest("AI_ASSET", 42L, "FACT_ERROR", null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_REPORT);
    }

    @Test
    void createReport_지원하지_않는_대상타입_INVALID_INPUT() {
        ReportCreateRequest request =
                new ReportCreateRequest("UNKNOWN_TARGET", 1L, "SPAM", null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_POST_대상_없으면_REPORT_TARGET_NOT_FOUND() {
        when(getSharingPostUseCase.getDetail(1L, 300L))
                .thenThrow(new BusinessException(ErrorCode.SHARING_POST_NOT_FOUND));

        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "SPAM", null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void createReport_POST_검증중_비대상예외는_그대로_전파() {
        // SHARING_POST_NOT_FOUND가 아닌 BusinessException은 REPORT_TARGET_NOT_FOUND로 둔갑시키지 않고 재던진다.
        when(getSharingPostUseCase.getDetail(1L, 300L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        ReportCreateRequest request =
                new ReportCreateRequest("POST", 300L, "SPAM", null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(reportRepository, never()).save(any(Report.class));
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
