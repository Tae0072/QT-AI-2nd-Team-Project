package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청 DTO.
 * Flutter SDK가 발급한 카카오 access token을 전달받는다.
 */
public record LoginRequest(
        @NotBlank(message = "카카오 access token은 필수입니다.")
        String kakaoAccessToken
) {}
