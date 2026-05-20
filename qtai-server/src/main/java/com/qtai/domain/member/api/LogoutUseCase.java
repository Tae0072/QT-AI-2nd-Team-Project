package com.qtai.domain.member.api;

/**
 * 회원 로그아웃 UseCase 포트.
 *
 * JWT는 무상태(stateless)지만 즉시 무효화를 위해 토큰 blacklist 또는
 * Refresh token 폐기 방식을 사용한다.
 */
public interface LogoutUseCase {

    // TODO: void logout(Long memberId, String accessToken);
    //       토큰을 만료 시각까지 blacklist에 기록 (Redis 권장) 또는 Refresh token 삭제
}
