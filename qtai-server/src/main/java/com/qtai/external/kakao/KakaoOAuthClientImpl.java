package com.qtai.external.kakao;

import com.qtai.external.kakao.dto.KakaoUserResponse;
import org.springframework.stereotype.Component;

/**
 * KakaoOAuthClient의 HTTP 구현체.
 *
 * 호출 대상: GET https://kapi.kakao.com/v2/user/me
 * 인증 헤더: Authorization: Bearer {accessToken}
 * 실패(4xx/5xx) → BusinessException(ErrorCode.UNAUTHORIZED) 변환.
 */
// TODO: @RequiredArgsConstructor 추가 후 RestTemplate(또는 WebClient) 주입
@Component
public class KakaoOAuthClientImpl implements KakaoOAuthClient {

    @Override
    public KakaoUserResponse fetchUser(String accessToken) {
        // TODO: fetchUser 구현
        //       1) Authorization 헤더에 Bearer {accessToken} 설정
        //       2) GET /v2/user/me 호출
        //       3) 응답 JSON → KakaoUserResponse 매핑 후 반환
        //       4) 예외 발생 시 BusinessException(UNAUTHORIZED) 던지기
        throw new UnsupportedOperationException("Kakao OAuth 호출은 member 도메인 PR에서 구현");
    }
}
