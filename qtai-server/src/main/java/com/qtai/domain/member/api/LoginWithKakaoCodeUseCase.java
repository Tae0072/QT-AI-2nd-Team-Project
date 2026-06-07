package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.KakaoCodeLoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;

/**
 * 카카오 웹 로그인(서버 OAuth, B안) UseCase 포트.
 *
 * <p>인가 코드 → 카카오 토큰 교환 → 기존 카카오 로그인(토큰 기반) 재사용 → JWT 발급.
 */
public interface LoginWithKakaoCodeUseCase {

    LoginResponse loginWithKakaoCode(KakaoCodeLoginRequest request);
}
