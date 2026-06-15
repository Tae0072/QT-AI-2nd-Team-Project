package com.qtai.domain.mission.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 미션 정의 수정 요청 (F-13). null 필드는 기존 값을 유지한다(부분 수정).
 * code는 식별자라 변경하지 않는다. 상태(status)는 별도 엔드포인트로 변경한다.
 */
public record MissionUpdateRequest(
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @Pattern(regexp = "MEDITATION_SAVED_DAYS|NOTE_SAVED_COUNT|STREAK_DAYS",
                message = "metricType이 올바르지 않습니다.")
        String metricType,

        @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "periodType이 올바르지 않습니다.")
        String periodType,

        @Min(value = 1, message = "targetCount는 1 이상이어야 합니다.")
        Integer targetCount
) {
}
