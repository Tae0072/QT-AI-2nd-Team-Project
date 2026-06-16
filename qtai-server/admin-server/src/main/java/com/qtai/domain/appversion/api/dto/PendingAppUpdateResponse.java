package com.qtai.domain.appversion.api.dto;

import java.time.LocalDateTime;

/**
 * 업데이트 예정 항목 응답 DTO.
 */
public record PendingAppUpdateResponse(
        Long id,
        String title,
        String description,
        String targetAppVersion,
        String updateMode,
        String status,
        LocalDateTime createdAt,
        LocalDateTime appliedAt
) {
}
