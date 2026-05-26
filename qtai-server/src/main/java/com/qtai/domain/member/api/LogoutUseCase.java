package com.qtai.domain.member.api;

/**
 * 회원 로그아웃 UseCase 포트.
 *
 * Refresh token을 Redis에서 삭제하여 토큰 갱신을 차단한다.
 */
public interface LogoutUseCase {

    void logout(Long memberId);
}
