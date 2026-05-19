package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;

/**
 * 카카오 OAuth 통신 포트.
 *
 * 도메인은 항상 이 인터페이스만 의존 — 네이버/구글 확장 또는 테스트용 Fake로
 * 교체할 때 도메인 변경 없이 구현체만 바꿔 끼울 수 있도록.
 */
public interface KakaoOAuthClient {

    // TODO: 카카오 access token으로 사용자 정보(id/email/nickname) 조회
    KakaoUserResponse fetchUser(String accessToken);
}
