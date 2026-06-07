package com.qtai.domain.member.api.dto;

/**
 * 사용자 설정 응답 DTO.
 *
 * @param notificationEnabled 알림 수신 여부
 * @param fontSize 폰트 크기 (SMALL / MEDIUM / LARGE)
 * @param musicEnabled 배경음악 켜기/끄기
 * @param musicVolume 배경음악 볼륨 (0~100)
 * @param musicCategory 배경음악 재생 대상 (ALL / BGM / HYMN)
 */
public record SettingsResponse(
        Boolean notificationEnabled,
        String fontSize,
        Boolean musicEnabled,
        Integer musicVolume,
        String musicCategory
) {}
