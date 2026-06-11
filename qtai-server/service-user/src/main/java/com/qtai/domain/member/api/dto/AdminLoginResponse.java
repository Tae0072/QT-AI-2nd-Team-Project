package com.qtai.domain.member.api.dto;

/**
 * 관리자 로그인 응답 DTO (계약 §3, 합의 완료).
 *
 * <p>사용자 로그인({@link LoginResponse})과 일관: 토큰은 body로 전달(앱·웹 동일, 합의 §3),
 * 회원 요약 키는 사용자=`member`/관리자=`admin`으로 구분(합의 §1).
 *
 * @param accessToken  JWT access token (role=ADMIN, 만료 사용자와 동일 30분)
 * @param refreshToken JWT refresh token (body 전달, 만료 14일)
 * @param admin        관리자 요약 정보
 */
public record AdminLoginResponse(
        String accessToken,
        String refreshToken,
        AdminSummary admin
) {

    /**
     * 관리자 요약. {@code adminRole}은 단일 역할 문자열(합의 §2).
     *
     * @param memberId  회원 PK
     * @param nickname  닉네임
     * @param role      회원 권한 (항상 ADMIN)
     * @param adminRole admin_users 세부 역할 (OPERATOR/REVIEWER/CONTENT_CREATOR/SUPER_ADMIN)
     * @param status    회원 상태 (ACTIVE 등)
     */
    public record AdminSummary(
            Long memberId,
            String nickname,
            String role,
            String adminRole,
            String status
    ) {
    }
}
