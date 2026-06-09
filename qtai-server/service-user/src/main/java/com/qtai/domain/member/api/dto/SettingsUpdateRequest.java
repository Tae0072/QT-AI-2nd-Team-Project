package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 사용자 설정 수정 요청 DTO.
 *
 * <p>PATCH — null 필드는 변경하지 않는다 (partial update).
 *
 * @param notificationEnabled 알림 수신 여부 (null이면 미변경)
 * @param fontSize 폰트 크기 SMALL/MEDIUM/LARGE (null이면 미변경)
 * @param musicEnabled 배경음악 켜기/끄기 (null이면 미변경)
 * @param musicVolume 배경음악 볼륨 0~100 (null이면 미변경)
 * @param musicCategory 배경음악 대상 ALL/BGM/HYMN (null이면 미변경)
 */
public record SettingsUpdateRequest(
        Boolean notificationEnabled,
        @Pattern(regexp = "SMALL|MEDIUM|LARGE", message = "fontSize는 SMALL, MEDIUM, LARGE 중 하나여야 합니다")
        String fontSize,
        Boolean musicEnabled,
        @Min(value = 0, message = "musicVolume은 0 이상이어야 합니다")
        @Max(value = 100, message = "musicVolume은 100 이하여야 합니다")
        Integer musicVolume,
        @Pattern(regexp = "ALL|BGM|HYMN", message = "musicCategory는 ALL, BGM, HYMN 중 하나여야 합니다")
        String musicCategory
) {}
