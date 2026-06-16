package com.qtai.domain.admin.api.dto;

/**
 * 관리자 로그인/토큰 갱신 결과.
 *
 * <p>토큰은 body로 전달한다(웹 SPA, HttpOnly 쿠키 미사용 — 현행 정책 유지).
 * 응답 형태는 기존 카카오 로그인 응답과 일관되게 access/refresh + admin 요약을 담는다.
 *
 * @param accessToken  ADMIN access token (role=ADMIN, 30분)
 * @param refreshToken refresh token (14일)
 * @param admin        관리자 요약
 */
public record AdminLoginResult(
        String accessToken,
        String refreshToken,
        Admin admin
) {

    /**
     * 관리자 요약.
     *
     * @param memberId  회원 PK
     * @param nickname  닉네임
     * @param role      회원 권한 (항상 ADMIN)
     * @param adminRole admin_users 세부 역할 (OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN)
     * @param status    회원 상태
     */
    public record Admin(
            Long memberId,
            String nickname,
            String role,
            String adminRole,
            String status
    ) {
    }
}
