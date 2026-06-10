package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 카카오 로그인 요청 DTO.
 *
 * <p>사용자 로그인({@link LoginRequest})과 동일하게 카카오 SDK가 발급한 access token을 받는다(계약 §2).
 * admin-web(브라우저)이 카카오 토큰을 전달한다.
 */
public record AdminLoginRequest(
        @NotBlank(message = "카카오 access token은 필수입니다.")
        String kakaoAccessToken
) {
}
