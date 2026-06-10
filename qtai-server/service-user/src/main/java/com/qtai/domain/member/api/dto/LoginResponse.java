package com.qtai.domain.member.api.dto;

/**
 * 로그인/토큰 갱신 응답 DTO.
 *
 * @param accessToken JWT access token
 * @param refreshToken JWT refresh token (HttpOnly 쿠키 대신 body로 전달 — Flutter SecureStorage 저장용)
 * @param member 회원 기본 정보
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        MemberSummary member
) {
    /**
     * 로그인 응답에 포함되는 회원 요약 정보.
     *
     * @param id 회원 PK
     * @param nickname 닉네임 (자동 생성 시 임시값)
     * @param role 권한 (USER, ADMIN)
     * @param status 상태 (ACTIVE, SUSPENDED, WITHDRAWN)
     * @param onboardingRequired 닉네임 미설정(자동 생성) 시 true
     */
    public record MemberSummary(
            Long id,
            String nickname,
            String role,
            String status,
            boolean onboardingRequired
    ) {}
}
