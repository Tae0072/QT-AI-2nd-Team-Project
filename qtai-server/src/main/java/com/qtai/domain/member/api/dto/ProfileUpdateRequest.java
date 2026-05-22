package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO.
 * Partial Update — null 필드는 변경하지 않는다.
 */
public record ProfileUpdateRequest(
        @Size(min = 2, max = 20) String nickname,
        @Size(max = 500) String profileImageUrl
) {}
