package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthClientImpl implements KakaoOAuthClient {

    @Override
    public KakaoUserResponse fetchUser(String accessToken) {
        throw new UnsupportedOperationException("Kakao OAuth 호출은 member 도메인 PR에서 구현");
    }
}
