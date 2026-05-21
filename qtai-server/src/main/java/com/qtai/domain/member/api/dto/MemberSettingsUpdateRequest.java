package com.qtai.domain.member.api.dto;

/**
 * 사용자 설정 수정 요청 DTO.
 *
 * PATCH /api/v1/me/settings (§4.1.7)
 * Partial Update: null 필드는 변경하지 않는다.
 *
 * @param verseSelectionMode 절 선택 방식 (null 이면 유지)
 * @param writingMode        글쓰기 방식 (null 이면 유지)
 */
public record MemberSettingsUpdateRequest(
        String verseSelectionMode,
        String writingMode
) {}
