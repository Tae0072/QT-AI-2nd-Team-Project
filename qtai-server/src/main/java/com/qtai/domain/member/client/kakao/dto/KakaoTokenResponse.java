package com.qtai.domain.member.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 토큰 응답 DTO (인가 코드 교환 결과).
 *
 * <p>웹 카카오 로그인(서버 OAuth, B안)에서 {@code POST https://kauth.kakao.com/oauth/token}
 * 응답을 매핑한다. snake_case 필드를 camelCase로 받는다.
 */
public record KakaoTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("scope") String scope
) {
}
