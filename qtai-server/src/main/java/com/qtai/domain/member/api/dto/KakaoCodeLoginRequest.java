package com.qtai.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 웹 로그인 요청 DTO (서버 OAuth, B안).
 *
 * <p>웹(브라우저)에서 Kakao.Auth.authorize 리다이렉트로 받은 인가 코드를 전달한다.
 * redirect_uri는 보안상 서버 설정값을 사용하므로 코드만 받는다.
 */
public record KakaoCodeLoginRequest(
        @NotBlank(message = "카카오 인가 코드는 필수입니다.")
        String code
) {}
