package com.qtai.domain.member.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 /v2/user/me 응답 DTO.
 *
 * 필요한 필드만 매핑하고, 나머지는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfo(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Profile(
                String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl
        ) {}
    }

    /**
     * 이메일 추출 (nullable).
     */
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    /**
     * 닉네임 추출 (nullable).
     */
    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.profile() != null) {
            return kakaoAccount.profile().nickname();
        }
        return null;
    }

    /**
     * 프로필 이미지 URL 추출 (nullable).
     */
    public String getProfileImageUrl() {
        if (kakaoAccount != null && kakaoAccount.profile() != null) {
            return kakaoAccount.profile().profileImageUrl();
        }
        return null;
    }
}
