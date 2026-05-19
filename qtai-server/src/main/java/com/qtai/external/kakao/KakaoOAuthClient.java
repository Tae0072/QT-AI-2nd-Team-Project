package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;

/**
 * TODO: Kakao OAuth 호출 — interface. member 도메인에서 직접 주입.
 */
public interface KakaoOAuthClient {

    KakaoUserResponse fetchUser(String accessToken);
}
