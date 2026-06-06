package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 사용자 설정 수정 요청 DTO.
 *
 * <p>PATCH — null 필드는 변경하지 않는다 (partial update).
 *
 * @param notificationEnabled 알림 수신 여부 (null이면 미변경)
 * @param fontSize 폰트 크기 SMALL/MEDIUM/LARGE (null이면 미변경)
 */
public record SettingsUpdateRequest(
        Boolean notificationEnabled,
        @Pattern(regexp = "SMALL|MEDIUM|LARGE", message = "fontSize는 SMALL, MEDIUM, LARGE 중 하나여야 합니다")
        String fontSize
) {}
