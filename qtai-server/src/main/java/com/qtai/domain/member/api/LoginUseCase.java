package com.qtai.domain.member.api;

/**
 * 회원 로그인 UseCase 포트.
 *
 * 흐름: 클라이언트가 받은 Kakao access token → KakaoOAuthClient로 검증 →
 * Member 조회 또는 신규 생성(첫 로그인) → JWT 발급.
 */
public interface LoginUseCase {

    // TODO: LoginResponse login(LoginRequest request);
    //       반환: JWT accessToken + MemberResponse
    //       내부에서 첫 로그인 시 자동 가입 처리 (member 행 INSERT)
}
