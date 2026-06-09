package com.qtai.domain.report.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 신고 목록 응답.
 */
public record AdminReportListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /**
     * 관리자 신고 목록 항목. 신고자/대상/사유/상태/처리정보를 노출(관리자 전용).
     */
    public record Item(
            Long id,
            Long reporterMemberId,
            String targetType,
            Long targetId,
            String reason,
            String detail,
            String status,
            Long processedByAdminId,
            LocalDateTime processedAt,
            LocalDateTime createdAt) {
    }
}
