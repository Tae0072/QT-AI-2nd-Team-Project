package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Access Token 재발급 요청 DTO.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refresh token은 필수입니다.")
        String refreshToken
) {}
