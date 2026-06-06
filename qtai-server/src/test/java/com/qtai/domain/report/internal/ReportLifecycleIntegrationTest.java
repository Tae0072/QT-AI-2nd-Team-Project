package com.qtai.domain.report.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.ListAdminReportsUseCase;
import com.qtai.domain.report.api.ProcessReportUseCase;
import com.qtai.domain.report.api.dto.AdminReportListQuery;
import com.qtai.domain.report.api.dto.AdminReportListResponse;
import com.qtai.domain.report.api.dto.ProcessReportCommand;
import com.qtai.domain.report.api.dto.ProcessReportResult;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 접수 → 관리자 조회 → 처리 라이프사이클 풀 컨텍스트 통합 테스트 (3단계 E2E).
 *
 * <p>기존 report 테스트는 슬라이스(@WebMvcTest)·단위(mock)뿐이라, 사용자 도메인(ReportService)과
 * 관리자 도메인(AdminReportService)이 같은 저장소를 통해 실제로 연결되는 흐름을 관통하지 않았다.
 * 이 테스트는 전체 ApplicationContext를 띄워 두 UseCase를 실제 빈으로 호출하고 H2에 영속된 상태를
 * 검증한다.
 *
 * <p>대상 검증(#147)은 POST(나눔글)만 sharing 도메인을 거치므로, 시드가 필요 없는 AI_ASSET/COMMENT
 * 대상으로 신고를 접수해 흐름 자체에 집중한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportLifecycleIntegrationTest {

    @Autowired
    private CreateReportUseCase createReportUseCase;
    @Autowired
    private ListAdminReportsUseCase listAdminReportsUseCase;
    @Autowired
    private ProcessReportUseCase processReportUseCase;
    @Autowired
    private ReportRepository reportRepository;

    @Test
    void 회원_신고접수_관리자_조회_처리완료까지_관통한다() {
        // 1) 회원이 신고 접수 (RECEIVED)
        ReportResponse created = createReportUseCase.createReport(
                700L, new ReportCreateRequest("AI_ASSET", 500L, "SPAM", "부적절한 산출물"));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo("RECEIVED");

        // 2) 관리자 목록 조회에 노출된다
        AdminReportListResponse list = listAdminReportsUseCase.listReports(
                new AdminReportListQuery(null, null, 0, 20));
        assertThat(list.content())
                .anyMatch(item -> item.id().equals(created.id())
                        && "RECEIVED".equals(item.status())
                        && "AI_ASSET".equals(item.targetType()));

        // 3) 관리자가 처리 완료(resolve)
        ProcessReportResult result = processReportUseCase.resolve(
                new ProcessReportCommand(9L, created.id(), "HIDE_TARGET", "정책 위반", true));
        assertThat(result.status()).isEqualTo("RESOLVED");
        assertThat(result.processedByAdminId()).isEqualTo(9L);

        // 4) 영속 상태(처리자/시각) 검증
        Report persisted = reportRepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(persisted.getProcessedByAdminId()).isEqualTo(9L);
        assertThat(persisted.getProcessedAt()).isNotNull();
    }

    @Test
    void 이미_처리된_신고는_재처리가_차단된다() {
        ReportResponse created = createReportUseCase.createReport(
                701L, new ReportCreateRequest("COMMENT", 600L, "ABUSE", null));
        processReportUseCase.resolve(new ProcessReportCommand(9L, created.id(), null, null, false));

        // 이미 RESOLVED → reject 재시도는 REPORT_ALREADY_PROCESSED로 차단
        assertThatThrownBy(() -> processReportUseCase.reject(
                new ProcessReportCommand(9L, created.id(), null, null, false)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_ALREADY_PROCESSED);
    }
}
