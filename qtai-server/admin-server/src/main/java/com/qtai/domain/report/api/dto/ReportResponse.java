package com.qtai.domain.report.api.dto;

import java.time.LocalDateTime;

/**
 * 신고 응답 DTO.
 *
 * <p>API 명세서 §4.4.7 — 접수 직후 식별자/상태/생성시각만 반환한다.
 * 신고자/대상/사유 같은 내부 정보는 사용자 응답에 노출하지 않는다.
 *
 * @param id        신고 ID
 * @param status    신고 상태 (접수 직후 RECEIVED)
 * @param createdAt 접수 시각
 */
public record ReportResponse(
        Long id,
        String status,
        LocalDateTime createdAt) {
}
