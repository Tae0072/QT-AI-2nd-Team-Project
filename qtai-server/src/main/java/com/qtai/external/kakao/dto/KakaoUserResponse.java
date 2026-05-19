package com.qtai.external.kakao.dto;

/**
 * TODO: Kakao 사용자 응답 DTO — id/email/nickname 등.
 */
public record KakaoUserResponse(Long id, String email, String nickname) {}
