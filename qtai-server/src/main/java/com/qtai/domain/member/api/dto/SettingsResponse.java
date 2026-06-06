package com.qtai.domain.member.api.dto;

/**
 * 사용자 설정 응답 DTO.
 *
 * @param notificationEnabled 알림 수신 여부
 * @param fontSize 폰트 크기 (SMALL / MEDIUM / LARGE)
 */
public record SettingsResponse(
        Boolean notificationEnabled,
        String fontSize
) {}
