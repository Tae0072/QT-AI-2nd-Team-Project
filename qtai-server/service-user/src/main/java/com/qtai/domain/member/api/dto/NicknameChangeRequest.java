package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 변경 요청 DTO.
 *
 * PATCH /api/v1/me/nickname
 * 즉시 변경 가능(2026-06-11 잠금 폐지).
 */
public record NicknameChangeRequest(
        @NotBlank @Size(min = 2, max = 20) String nickname
) {}
