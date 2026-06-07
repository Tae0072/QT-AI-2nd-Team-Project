package com.qtai.domain.member.internal;

import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.LoginWithKakaoCodeUseCase;
import com.qtai.domain.member.api.dto.KakaoCodeLoginRequest;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.client.kakao.KakaoTokenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오 웹 로그인 서비스 (서버 OAuth, B안).
 *
 * <p>인가 코드를 카카오 토큰으로 교환한 뒤, <b>기존</b> {@link LoginUseCase#login}(토큰 기반)을
 * 그대로 재사용한다 — 회원 조회/가입/재활성화/JWT 발급 로직 중복을 피한다.
 *
 * <p><b>DRAFT</b>: 서버사이드 OAuth는 CLAUDE.md §1 결정과 충돌(머지 전 강사/Lead 검토).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoWebAuthService implements LoginWithKakaoCodeUseCase {

    private final KakaoTokenClient kakaoTokenClient;
    private final LoginUseCase loginUseCase;

    @Override
    public LoginResponse loginWithKakaoCode(KakaoCodeLoginRequest request) {
        String kakaoAccessToken = kakaoTokenClient.getAccessTokenByCode(request.code());
        return loginUseCase.login(new LoginRequest(kakaoAccessToken));
    }
}
