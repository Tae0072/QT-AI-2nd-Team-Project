package com.qtai.domain.mission.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관리자 미션 정의 생성 요청 (F-13).
 */
public record MissionCreateRequest(
        @NotBlank(message = "code는 필수입니다.")
        @Size(max = 50, message = "code는 50자 이하여야 합니다.")
        String code,

        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "metricType은 필수입니다.")
        @Pattern(regexp = "MEDITATION_SAVED_DAYS|NOTE_SAVED_COUNT|STREAK_DAYS",
                message = "metricType이 올바르지 않습니다.")
        String metricType,

        @NotBlank(message = "periodType은 필수입니다.")
        @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "periodType이 올바르지 않습니다.")
        String periodType,

        @NotNull(message = "targetCount는 필수입니다.")
        @Min(value = 1, message = "targetCount는 1 이상이어야 합니다.")
        Integer targetCount
) {
}
