package com.qtai.external.kakao.dto;

/** 카카오 /v2/user/me 응답 매핑 DTO. */
public record KakaoUserResponse(
        // TODO: Long id                — 카카오 회원 고유 ID
        // TODO: String email           — 이메일 (선택 동의 항목, null 가능)
        // TODO: String nickname        — 카카오 닉네임
        // TODO: String profileImageUrl — 프로필 이미지 URL (추가 필요)
        Long id,
        String email,
        String nickname
) {}
