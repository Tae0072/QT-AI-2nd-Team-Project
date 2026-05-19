package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;

public interface KakaoOAuthClient {

    KakaoUserResponse fetchUser(String accessToken);
}
