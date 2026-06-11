package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 변경 요청 DTO.
 *
 * PATCH /api/v1/me/nickname
 * 7일 잠금 정책 적용.
 */
public record NicknameChangeRequest(
        @NotBlank @Size(min = 2, max = 20) String nickname
) {}
