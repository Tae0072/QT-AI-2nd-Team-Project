package com.qtai.domain.member.api.dto;

/** 로그인 요청 DTO. */
public record LoginRequest(
        // TODO: String kakaoAccessToken — 클라이언트가 카카오 SDK로 받은 access token (필수)
        // 또는 authCode 방식 사용 시: String authCode
) {}
