package com.qtai.domain.report.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.report.api.CreateReportUseCase;
import com.qtai.domain.report.api.dto.ReportCreateRequest;
import com.qtai.domain.report.api.dto.ReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 신고 REST 엔드포인트.
 *
 * <p>API 명세서 §4.4.7 기준. base path: /api/v1/reports
 * <ul>
 *   <li>POST /api/v1/reports — 신고 접수 (USER, 연결 화면 S-03/Q-04)</li>
 * </ul>
 *
 * <p>관리자 신고 처리(GET /api/v1/admin/reports, resolve)는 admin 도메인에서 제공한다.
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final CreateReportUseCase createReportUseCase;

    /** POST /api/v1/reports — 신고 접수. */
    @PostMapping("/api/v1/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse response = createReportUseCase.createReport(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
