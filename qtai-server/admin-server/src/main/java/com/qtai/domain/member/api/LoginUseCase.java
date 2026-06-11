package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;

/**
 * 회원 로그인 UseCase 포트.
 *
 * 흐름: 클라이언트가 받은 Kakao access token -> KakaoOAuthClient로 검증 ->
 * Member 조회 또는 신규 생성(첫 로그인) -> JWT 발급.
 */
public interface LoginUseCase {

    LoginResponse login(LoginRequest request);
}
