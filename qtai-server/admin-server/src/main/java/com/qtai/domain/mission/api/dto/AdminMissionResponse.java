package com.qtai.domain.mission.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 미션 정의 응답 DTO (F-13).
 *
 * <p>도메인 경계 정책: api/dto는 internal을 import하지 않는다(enum은 String).
 */
public record AdminMissionResponse(
        Long id,
        String code,
        String title,
        String metricType,
        String periodType,
        Integer targetCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
