package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;
import org.springframework.stereotype.Component;

/**
 * TODO: Kakao /v2/user/me REST 호출 구현 — accessToken으로 사용자 프로필 조회.
 */
@Component
public class KakaoOAuthClientImpl implements KakaoOAuthClient {

    @Override
    public KakaoUserResponse fetchUser(String accessToken) {
        throw new UnsupportedOperationException("Kakao OAuth 호출은 member 도메인 PR에서 구현");
    }
}
