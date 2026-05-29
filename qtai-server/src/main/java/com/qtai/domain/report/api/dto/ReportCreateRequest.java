package com.qtai.domain.report.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 신고 생성 요청 DTO.
 *
 * <p>API 명세서 §4.4.7 (POST /api/v1/reports).
 *
 * @param targetType 신고 대상 타입 (POST / COMMENT / AI_QA_REQUEST / AI_ASSET)
 * @param targetId   신고 대상 ID
 * @param reason     신고 사유 코드 (VARCHAR(50))
 * @param detail     상세 사유 (선택)
 */
public record ReportCreateRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 50) String reason,
        String detail) {
}
