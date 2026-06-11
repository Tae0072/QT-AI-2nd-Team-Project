package com.qtai.domain.report.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 신고 처리 결과.
 */
public record ProcessReportResult(
        Long reportId,
        String status,
        Long processedByAdminId,
        LocalDateTime processedAt) {
}
