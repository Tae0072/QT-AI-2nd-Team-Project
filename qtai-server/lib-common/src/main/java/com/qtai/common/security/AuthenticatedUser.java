package com.qtai.common.security;

/**
 * JWT에서 검증·추출한 인증 주체. 서비스 간 공통 사용자 식별자({@code memberId})를 담는다.
 *
 * @param memberId 회원 PK (토큰 sub)
 * @param role     회원 권한 (예: "USER", "ADMIN")
 */
public record AuthenticatedUser(long memberId, String role) {
}
